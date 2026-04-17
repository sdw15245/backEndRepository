package com.sweep.project.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.route.BoardingInfo;
import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.TrafficResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 경로(route) 및 탑승 정보(boarding)를 Redis Hash 타입으로 캐싱하는 서비스.
 *
 * <h3>Redis 구조</h3>
 * <pre>
 * [route] — Hash
 *   Key  : route:{type}:{startLat:.4f}:{startLon:.4f}:{endLat:.4f}:{endLon:.4f}
 *   Field: {routeId}          ← DB Route PK
 *   Value: JSON(TrafficResponse 단일)
 *   TTL  : 자정까지
 *
 *   예) route:subway:37.6134:126.9265:37.5004:127.1269
 *         "42" → JSON(SubwayRoute1)
 *         "43" → JSON(SubwayRoute2)
 *         "44" → JSON(SubwayRoute3)
 *
 * [boarding] — Hash  (route 와 동일한 routeId 를 subkey 로 사용)
 *   Key  : boarding:{type}:{HHmm}:{dayCode}:{startLat:.4f}:{startLon:.4f}:{endLat:.4f}:{endLon:.4f}
 *   Field: {routeId}          ← route hash 와 동일한 PK → 순서·연관 보장
 *   Value: JSON(BoardingInfo 단일)
 *   TTL  : 20분
 *
 *   예) boarding:subway:0900:1:37.6134:126.9265:37.5004:127.1269
 *         "42" → JSON(BoardingInfo1)
 *         "43" → JSON(BoardingInfo2)
 *         "44" → JSON(BoardingInfo3)
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    @Qualifier("redisObjectMapper")
    private final ObjectMapper redisObjectMapper;

    private static final long BOARDING_TTL_SECONDS = TimeUnit.MINUTES.toSeconds(20L);

    private static final TypeReference<TrafficResponse> ROUTE_TYPE      = new TypeReference<>() {};
    private static final TypeReference<BoardingInfo>    BOARDING_TYPE   = new TypeReference<>() {};

    /**
     * Hash multi-field save-if-absent Lua 스크립트 (route / boarding 공용).
     * <pre>
     * KEYS[1]   = hash 키
     * ARGV[1]   = TTL(초)
     * ARGV[2,3] = field1, value1
     * ARGV[4,5] = field2, value2  ...
     * </pre>
     * 키가 없을 때만 모든 field-value 를 HSET 하고 EXPIRE 설정.
     */
    private static final RedisScript<Long> HASH_SAVE_IF_ABSENT = RedisScript.of(
            "if redis.call('EXISTS', KEYS[1]) == 0 then " +
            "  local i = 2 " +
            "  while i <= #ARGV do " +
            "    redis.call('HSET', KEYS[1], ARGV[i], ARGV[i+1]) " +
            "    i = i + 2 " +
            "  end " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "  return 1 " +
            "end " +
            "return 0",
            Long.class
    );

    // ── 경로 캐시 ─────────────────────────────────────────────────────────────

    /**
     * 각 route 를 routeId 를 hash field 로 하여 개별 저장한다.
     * 키가 이미 존재하면 저장하지 않는다.
     *
     * @param routeIds      DB Route PK 목록 (routeJsonList 와 인덱스 일치)
     * @param routeJsonList 각 route 의 JSON 목록
     */
    public boolean saveIfAbsent(PathSearchType type,
                                double startLat, double startLon,
                                double endLat, double endLon,
                                List<Long> routeIds,
                                List<String> routeJsonList) {
        if (routeIds.isEmpty()) return false;
        String key = buildRouteKey(type, startLat, startLon, endLat, endLon);
        int secondLeft = secondsUntilMidnight();

        List<String> argv = buildArgv(secondLeft, routeIds, routeJsonList);
        Long result = stringRedisTemplate.execute(
                HASH_SAVE_IF_ABSENT, Collections.singletonList(key), argv.toArray(new String[0]));
        boolean saved = Long.valueOf(1L).equals(result);
        log.info("[Redis][route] {} key={} routeIds={} saved={}", type, key, routeIds, saved);
        return saved;
    }

    /**
     * route Hash 에서 전체 경로를 routeId 오름차순으로 정렬하여 반환한다.
     */
    public Optional<List<TrafficResponse>> find(PathSearchType type,
                                                double startLat, double startLon,
                                                double endLat, double endLon) {
        String key = buildRouteKey(type, startLat, startLon, endLat, endLon);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            log.debug("[Redis][route] 캐시 미스 key={}", key);
            return Optional.empty();
        }
        List<TrafficResponse> routes = entriesSortedById(entries).stream()
                .map(v -> deserialize(v, ROUTE_TYPE, key))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.info("[Redis][route] 캐시 히트 key={} count={}", key, routes.size());
        return Optional.of(routes);
    }

    /**
     * routeId 로 단일 route 를 조회한다.
     * Redis 미스 시 dbRouteJson 을 재캐싱한 뒤 반환한다.
     */
    public Optional<TrafficResponse> findOrCacheRoute(PathSearchType type,
                                                       double startLat, double startLon,
                                                       double endLat, double endLon,
                                                       Long routeId,
                                                       String dbRouteJson) {
        String key = buildRouteKey(type, startLat, startLon, endLat, endLon);
        String json = (String) stringRedisTemplate.opsForHash().get(key, String.valueOf(routeId));
        if (json != null) {
            log.info("[Redis][route] 단일 히트 key={} routeId={}", key, routeId);
            return Optional.ofNullable(deserialize(json, ROUTE_TYPE, key));
        }
        if (dbRouteJson != null) {
            stringRedisTemplate.opsForHash().putIfAbsent(key, String.valueOf(routeId), dbRouteJson);
            stringRedisTemplate.expire(key, secondsUntilMidnight(), TimeUnit.SECONDS);
            log.info("[Redis][route] DB fallback 재캐싱 key={} routeId={}", key, routeId);
            return Optional.ofNullable(deserialize(dbRouteJson, ROUTE_TYPE, key));
        }
        return Optional.empty();
    }

    /**
     * route Hash 에 저장된 routeId 목록을 오름차순으로 반환한다.
     * boarding 저장 시 동일한 순서·ID 를 사용하기 위해 호출한다.
     */
    public List<Long> getRouteIds(PathSearchType type,
                                  double startLat, double startLon,
                                  double endLat, double endLon) {
        String key = buildRouteKey(type, startLat, startLon, endLat, endLon);
        return stringRedisTemplate.opsForHash().keys(key).stream()
                .map(k -> Long.parseLong((String) k))
                .sorted()
                .collect(Collectors.toList());
    }

    // ── 탑승 정보 캐시 ────────────────────────────────────────────────────────

    /**
     * 각 boardingInfo 를 routeId 를 hash field 로 하여 개별 저장한다.
     * route hash 와 동일한 routeId·순서를 사용하므로 인덱스 연관이 보장된다.
     *
     * @param routeIds     route hash 의 routeId 목록 (boardingInfos 와 인덱스 일치)
     * @param boardingInfos 저장할 탑승 정보 목록
     */
    public boolean saveBoardingIfAbsent(PathSearchType type,
                                        String timeHHmm, int dayCode,
                                        double startLat, double startLon,
                                        double endLat, double endLon,
                                        List<BoardingInfo> boardingInfos,
                                        List<Long> routeIds) {
        if (routeIds.isEmpty() || boardingInfos.isEmpty()) return false;

        String key = buildBoardingKey(type, timeHHmm, dayCode, startLat, startLon, endLat, endLon);

        int pairCount = Math.min(routeIds.size(), boardingInfos.size());
        List<String> jsonList = new ArrayList<>(pairCount);
        for (int i = 0; i < pairCount; i++) {
            try {
                jsonList.add(redisObjectMapper.writeValueAsString(boardingInfos.get(i)));
            } catch (JsonProcessingException e) {
                log.error("[Redis][boarding] 직렬화 실패 index={}", i, e);
                jsonList.add("{}");
            }
        }

        List<Long>   ids  = routeIds.subList(0, pairCount);
        List<String> argv = buildArgv(BOARDING_TTL_SECONDS, ids, jsonList);
        Long result = stringRedisTemplate.execute(
                HASH_SAVE_IF_ABSENT, Collections.singletonList(key), argv.toArray(new String[0]));
        boolean saved = Long.valueOf(1L).equals(result);
        log.info("[Redis][boarding] {} key={} routeIds={} saved={}", type, key, ids, saved);
        return saved;
    }

    /**
     * boarding Hash 에서 전체 탑승 정보를 routeId 오름차순으로 정렬하여 반환한다.
     */
    public Optional<List<BoardingInfo>> findBoarding(PathSearchType type,
                                                     String timeHHmm, int dayCode,
                                                     double startLat, double startLon,
                                                     double endLat, double endLon) {
        String key = buildBoardingKey(type, timeHHmm, dayCode, startLat, startLon, endLat, endLon);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            log.debug("[Redis][boarding] 캐시 미스 key={}", key);
            return Optional.empty();
        }
        List<BoardingInfo> boardingInfos = entriesSortedById(entries).stream()
                .map(v -> deserialize(v, BOARDING_TYPE, key))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.info("[Redis][boarding] 캐시 히트 key={} count={}", key, boardingInfos.size());
        return Optional.of(boardingInfos);
    }

    // ── private ───────────────────────────────────────────────────────────────

    /** HGETALL 결과를 routeId(field) 오름차순으로 정렬한 value 목록 반환 */
    private List<String> entriesSortedById(Map<Object, Object> entries) {
        return entries.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> Long.parseLong((String) e.getKey())))
                .map(e -> (String) e.getValue())
                .collect(Collectors.toList());
    }

    /** Lua ARGV 배열 구성: [TTL, field1, val1, field2, val2, ...] */
    private List<String> buildArgv(long ttlSeconds, List<Long> ids, List<String> jsonList) {
        List<String> argv = new ArrayList<>(1 + ids.size() * 2);
        argv.add(String.valueOf(ttlSeconds));
        for (int i = 0; i < ids.size(); i++) {
            argv.add(String.valueOf(ids.get(i)));
            argv.add(jsonList.get(i));
        }
        return argv;
    }

    private <T> T deserialize(String json, TypeReference<T> type, String key) {
        try {
            return redisObjectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("[Redis] 역직렬화 실패 key={}", key, e);
            return null;
        }
    }

    private int secondsUntilMidnight() {
        return LocalTime.of(23, 59, 59).toSecondOfDay() - LocalTime.now().toSecondOfDay();
    }

    private String buildRouteKey(PathSearchType type,
                                 double startLat, double startLon,
                                 double endLat, double endLon) {
        return String.format("route:%s:%.4f:%.4f:%.4f:%.4f",
                toTypeName(type), startLat, startLon, endLat, endLon);
    }

    private String buildBoardingKey(PathSearchType type,
                                    String timeHHmm, int dayCode,
                                    double startLat, double startLon,
                                    double endLat, double endLon) {
        return String.format("boarding:%s:%s:%d:%.4f:%.4f:%.4f:%.4f",
                toTypeName(type), timeHHmm, dayCode, startLat, startLon, endLat, endLon);
    }

    // ── 버스 정류소 순번(ord) 캐시 ────────────────────────────────────────────

    /**
     * providerCode + busRouteId + stId 를 키로 ord(정류소 순번)를 저장한다.
     *
     * <pre>
     * Key  : bus:ord:{providerCode}:{busRouteId}:{stId}
     * Value: ord (문자열)
     * TTL  : 7일 (정류소 순번은 노선 변경 시에만 바뀌므로 장기 캐싱)
     * </pre>
     */
    public void saveOrd(int providerCode, String busRouteId, String stId, int ord) {
        String key = buildOrdKey(providerCode, busRouteId, stId);
        stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(ord), 30, TimeUnit.MINUTES);
        log.info("[Redis][ord] 저장 key={} ord={}", key, ord);
    }

    /**
     * providerCode + busRouteId + stId 키로 ord 를 조회한다.
     *
     * @return 캐시 히트 시 ord 값, 미스 시 {@link java.util.OptionalInt#empty()}
     */
    public java.util.OptionalInt getOrd(int providerCode, String busRouteId, String stId) {
        String key = buildOrdKey(providerCode, busRouteId, stId);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            log.debug("[Redis][ord] 캐시 미스 key={}", key);
            return java.util.OptionalInt.empty();
        }
        log.info("[Redis][ord] 캐시 히트 key={} ord={}", key, value);
        return java.util.OptionalInt.of(Integer.parseInt(value));
    }

    private String buildOrdKey(int providerCode, String busRouteId, String stId) {
        return "bus:ord:" + providerCode + ":" + busRouteId + ":" + stId;
    }

    public static String toTypeName(PathSearchType type) {
        return switch (type) {
            case PATH_TYPE_SUBWAY -> "subway";
            case PATH_TYPE_BUS    -> "bus";
            case PATH_TYPE_ANYONE -> "mixed";
        };
    }
}
