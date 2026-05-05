package com.sweep.project.route.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.route.TrafficResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 경로(Route) 엔티티의 DB 저장/조회를 담당하는 서비스.
 * 좌표는 Redis 키 생성({@code %.4f})과 동일하게 소수점 4자리 반올림 후 처리한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RouteDbService {

    private final RouteRepository routeRepository;
    private final ObjectMapper objectMapper;
    @Qualifier("redisObjectMapper")
    private final ObjectMapper redisObjectMapper;

    private static final TypeReference<TrafficResponse> ROUTE_TYPE = new TypeReference<>() {};
    /**
     * 각 route JSON 을 개별 행으로 저장하고 생성된 PK 목록을 반환한다.
     */
    public List<Long> saveAll(PathSearchType type,
                              double startLat, double startLon,
                              double endLat, double endLon,
                              List<String> routeJsonList) {

        double sx = round4(startLon), sy = round4(startLat);
        double ex = round4(endLon),   ey = round4(endLat);

        List<Long> ids = new ArrayList<>();
        for (String routeJson : routeJsonList) {
            Integer totalTime = parseTotalTime(routeJson);
            Route saved = routeRepository.save(
                    Route.builder()
                            .type(type)
                            .startX(sx).startY(sy)
                            .endX(ex).endY(ey)
                            .routeData(routeJson)
                            .totalTime(totalTime)
                            .build());
            ids.add(saved.getId());
            log.info("[RouteDb] 저장 id={} type={} totalTime={} coords=[{},{}→{},{}]",
                    saved.getId(), type, totalTime, sx, sy, ex, ey);
        }
        return ids;
    }

    /**
     * (type + 반올림 좌표) 로 저장된 모든 Route 를 조회한다.
     * Redis 캐시 미스 시 DB fallback 으로 사용한다.
     */
    @Transactional(readOnly = true)
    public List<RouteWithId> findRoutes(PathSearchType type,
                                        double startLat, double startLon,
                                        double endLat, double endLon) {
        return routeRepository
                .findByTypeAndStartXAndStartYAndEndXAndEndY(
                        type,
                        round4(startLon), round4(startLat),
                        round4(endLon), round4(endLat))
                .stream()
                .map(r -> new RouteWithId(r.getId(), r.getRouteData()))
                .collect(Collectors.toList());
    }

    /** DB 조회 결과를 담는 레코드 */
    public record RouteWithId(Long id, String routeJson) {}

    /**
     * (type + 반올림 좌표) 의 가장 최신 Route PK 를 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<Long> findLatestId(PathSearchType type,
                                       double startLat, double startLon,
                                       double endLat, double endLon) {
        return routeRepository
                .findFirstByTypeAndStartXAndStartYAndEndXAndEndYOrderByIdDesc(
                        type,
                        round4(startLon), round4(startLat),
                        round4(endLon), round4(endLat))
                .map(Route::getId);
    }

    @Transactional
    public void updateJsons(List<Long> routeIds, List<String> routeJsonList) {
        List<Route> routes = routeRepository.findAllById(routeIds);

        for (Route r : routes) {
            int idx = routeIds.indexOf(r.getId());
            if (idx >= 0) {
                r.updateRouteJson(routeJsonList.get(idx));
            }
        }
        // dirty checking으로 자동 update
    }

    public List<TrafficResponse> findById(Long id){
        Route route=routeRepository.findById(id)
                .orElseThrow(()->new RuntimeException("없는 루트입니다"));
        TrafficResponse trafficResponse=deserializeQuietly(route.getRouteData());
        return List.of(trafficResponse);
    }


    /** 소수점 4자리 반올림 — Redis {@code %.4f} 와 동일 */
    public static double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    /** routeData JSON 에서 totalTime(분) 추출. 파싱 실패 시 null 반환 */
    private Integer parseTotalTime(String routeJson) {
        if (routeJson == null) return null;
        try {
            return objectMapper.readTree(routeJson).path("totalTime").asInt(0);
        } catch (Exception e) {
            log.warn("[RouteDb] totalTime 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private TrafficResponse deserializeQuietly(String json) {
        try {
            return redisObjectMapper.readValue(json, ROUTE_TYPE);
        } catch (JsonProcessingException e) {
            log.error("[GeoCache] route 역직렬화 실패", e);
            return null;
        }
    }
}
