package com.sweep.project.route.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 각 route JSON 을 개별 행으로 저장하고 생성된 PK 목록을 반환한다.
     */
    public List<Long> saveAll(PathSearchType type,
                              double startLon, double startLat,
                              double endLon, double endLat,
                              List<String> routeJsonList) {

        double sx = round4(startLon), sy = round4(startLat);
        double ex = round4(endLon),   ey = round4(endLat);

        List<Long> ids = new ArrayList<>();
        for (String routeJson : routeJsonList) {
            Route saved = routeRepository.save(
                    Route.builder()
                            .type(type)
                            .startX(sx).startY(sy)
                            .endX(ex).endY(ey)
                            .routeData(routeJson)
                            .build());
            ids.add(saved.getId());
            log.info("[RouteDb] 저장 id={} type={} coords=[{},{}→{},{}]",
                    saved.getId(), type, sx, sy, ex, ey);
        }
        return ids;
    }

    /**
     * (type + 반올림 좌표) 로 저장된 모든 Route 를 조회한다.
     * Redis 캐시 미스 시 DB fallback 으로 사용한다.
     */
    @Transactional(readOnly = true)
    public List<RouteWithId> findRoutes(PathSearchType type,
                                        double startLon, double startLat,
                                        double endLon, double endLat) {
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
                                       double startLon, double startLat,
                                       double endLon, double endLat) {
        return routeRepository
                .findFirstByTypeAndStartXAndStartYAndEndXAndEndYOrderByIdDesc(
                        type,
                        round4(startLon), round4(startLat),
                        round4(endLon), round4(endLat))
                .map(Route::getId);
    }

    /** 소수점 4자리 반올림 — Redis {@code %.4f} 와 동일 */
    public static double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
