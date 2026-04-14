package com.sweep.project.route.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.route.BoardingInfo;
import com.sweep.project.route.PathSearchType;
import com.sweep.project.route.TrafficResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 파싱된 경로 목록(List&lt;TrafficResponse&gt;)을 Geolocation 기반 Redis 키에 저장하고 조회하는 서비스.
 *
 * <p>키 형식: {@code route:{type}:{startLat:.4f}:{startLon:.4f}:{endLat:.4f}:{endLon:.4f}}
 * <ul>
 *   <li>type = subway | bus | mixed</li>
 *   <li>좌표는 소수점 4자리(약 11 m 정밀도)로 반올림하여 미세한 GPS 오차를 흡수한다.</li>
 * </ul>
 *
 * <p>저장 시에는 Lua 스크립트로 EXISTS 체크 후 없을 때만 SET + EXPIRE 하여
 * 동시 요청에도 중복 저장을 방지한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    @Qualifier("redisObjectMapper")
    private final ObjectMapper redisObjectMapper;

    private static final long TTL_SECONDS = TimeUnit.MINUTES.toSeconds(20L);

    private static final TypeReference<List<TrafficResponse>> ROUTE_LIST_TYPE =
            new TypeReference<>() {};

    private static final TypeReference<List<BoardingInfo>> BOARDING_LIST_TYPE =
            new TypeReference<>() {};

    /**
     * Lua 스크립트.
     * KEYS[1] = Redis 키
     * ARGV[1] = JSON 직렬화된 List<TrafficResponse>
     * ARGV[2] = TTL(초)
     *
     * 키가 없을 때만 SET + EXPIRE → return 1 (저장됨)
     * 키가 이미 있으면 아무것도 하지 않음 → return 0 (스킵)
     */
    private static final RedisScript<Long> SAVE_IF_ABSENT_SCRIPT = RedisScript.of(
            "if redis.call('EXISTS', KEYS[1]) == 0 then " +
            "  redis.call('SET', KEYS[1], ARGV[1]) " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
            "  return 1 " +
            "end " +
            "return 0",
            Long.class
    );

    /**
     * 파싱된 경로 목록을 Redis 에 저장한다. 키가 이미 존재하면 저장하지 않는다.
     *
     * @param type     경로 유형 (subway / bus / mixed)
     * @param routes   저장할 List&lt;? extends TrafficResponse&gt;
     * @return true = 새로 저장됨, false = 이미 존재하여 스킵
     */
    public boolean saveIfAbsent(PathSearchType type,
                                double startLat, double startLon,
                                double endLat, double endLon,
                                List<? extends TrafficResponse> routes) {
        String key = buildKey(type, startLat, startLon, endLat, endLon);
        try {
            LocalTime now=LocalTime.now();
            LocalTime midnight = LocalTime.of(23, 59, 59);

            int secondLeft=midnight.toSecondOfDay()-now.toSecondOfDay();

            String json = redisObjectMapper.writeValueAsString(routes);
            Long result = stringRedisTemplate.execute(
                    SAVE_IF_ABSENT_SCRIPT,
                    Collections.singletonList(key),
                    json,
                    String.valueOf(secondLeft)
            );
            boolean saved = Long.valueOf(1L).equals(result);
            log.info("[Redis] {} key={} saved={}", type, key, saved);
            return saved;
        } catch (JsonProcessingException e) {
            log.error("[Redis] 직렬화 실패 key={}", key, e);
            return false;
        }
    }

    /**
     * Redis 에 저장된 경로 목록을 조회한다.
     *
     * @return 캐시 히트 시 Optional&lt;List&lt;TrafficResponse&gt;&gt;, 미스 시 Optional.empty()
     */
    public Optional<List<TrafficResponse>> find(PathSearchType type,
                                                double startLat, double startLon,
                                                double endLat, double endLon) {
        String key = buildKey(type, startLat, startLon, endLat, endLon);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            log.debug("[Redis] 캐시 미스 key={}", key);
            return Optional.empty();
        }
        try {
            List<TrafficResponse> routes = redisObjectMapper.readValue(json, ROUTE_LIST_TYPE);
            log.info("[Redis] 캐시 히트 key={}", key);
            return Optional.of(routes);
        } catch (JsonProcessingException e) {
            log.error("[Redis] 역직렬화 실패 key={}", key, e);
            return Optional.empty();
        }
    }

    // ── 탑승 정보 캐시 ───────────────────────────────────────────────────────

    /**
     * getBoardingInfo 결과(List&lt;BoardingInfo&gt;)를 Redis 에 저장한다. 키가 이미 존재하면 저장하지 않는다.
     *
     * @param type      경로 유형 (subway / bus / mixed)
     * @param startLat  출발지 위도
     * @param startLon  출발지 경도
     * @param endLat    도착지 위도
     * @param endLon    도착지 경도
     * @return true = 새로 저장됨, false = 이미 존재하여 스킵
     */
    public boolean saveBoardingIfAbsent(PathSearchType type,
                                        String timeHHmm, int dayCode,
                                        double startLat, double startLon,
                                        double endLat, double endLon,
                                        List<BoardingInfo> boardingInfos) {
        String key = buildBoardingKey(type, timeHHmm, dayCode, startLat, startLon, endLat, endLon);
        try {
            String json = redisObjectMapper.writeValueAsString(boardingInfos);
            Long result = stringRedisTemplate.execute(
                    SAVE_IF_ABSENT_SCRIPT,
                    Collections.singletonList(key),
                    json,
                    String.valueOf(TTL_SECONDS)
            );
            boolean saved = Long.valueOf(1L).equals(result);
            log.info("[Redis] boarding {} key={} saved={}", type, key, saved);
            return saved;
        } catch (JsonProcessingException e) {
            log.error("[Redis] boarding 직렬화 실패 key={}", key, e);
            return false;
        }
    }

    /**
     * Redis 에 저장된 탑승 정보를 조회한다.
     *
     * @return 캐시 히트 시 Optional&lt;List&lt;BoardingInfo&gt;&gt;, 미스 시 Optional.empty()
     */
    public Optional<List<BoardingInfo>> findBoarding(PathSearchType type,
                                                     String timeHHmm, int dayCode,
                                                     double startLat, double startLon,
                                                     double endLat, double endLon) {
        String key = buildBoardingKey(type, timeHHmm, dayCode, startLat, startLon, endLat, endLon);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            log.debug("[Redis] boarding 캐시 미스 key={}", key);
            return Optional.empty();
        }
        try {
            List<BoardingInfo> boardingInfos = redisObjectMapper.readValue(json, BOARDING_LIST_TYPE);
            log.info("[Redis] boarding 캐시 히트 key={}", key);
            return Optional.of(boardingInfos);
        } catch (JsonProcessingException e) {
            log.error("[Redis] boarding 역직렬화 실패 key={}", key, e);
            return Optional.empty();
        }
    }

    // ── private ──────────────────────────────────────────────────────────────

    /**
     * 경로 유형 + 좌표 기반으로 Redis 키를 생성한다.
     * 좌표는 소수점 4자리로 반올림하여 미세한 GPS 오차를 흡수한다.
     *
     * 예) route:subway:37.6134:126.9265:37.5004:127.1269
     */
    private String buildKey(PathSearchType type,
                             double startLat, double startLon,
                             double endLat, double endLon) {
        String typeName = switch (type) {
            case PATH_TYPE_SUBWAY -> "subway";
            case PATH_TYPE_BUS    -> "bus";
            case PATH_TYPE_ANYONE -> "mixed";
        };
        return String.format("route:%s:%.4f:%.4f:%.4f:%.4f",
                typeName, startLat, startLon, endLat, endLon);
    }

    /**
     * 탑승 정보 캐시 키를 생성한다.
     * cardinality expand를 막기위해서 캐싱시 위도 경도를 소수점 4자리까지 끊어서 묶자.
     * 예) boarding:subway:0900:1:37.6134:126.9265:37.5004:127.1269
     */
    private String buildBoardingKey(PathSearchType type,
                                    String timeHHmm, int dayCode,
                                    double startLat, double startLon,
                                    double endLat, double endLon) {
        String typeName = switch (type) {
            case PATH_TYPE_SUBWAY -> "subway";
            case PATH_TYPE_BUS    -> "bus";
            case PATH_TYPE_ANYONE -> "mixed";
        };
        return String.format("boarding:%s:%s:%d:%.4f:%.4f:%.4f:%.4f",
                typeName, timeHHmm, dayCode, startLat, startLon, endLat, endLon);
    }
}
