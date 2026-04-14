package com.sweep.project.route.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    /** (type + 좌표) 로 저장된 모든 Route 조회 */
    List<Route> findByTypeAndStartXAndStartYAndEndXAndEndY(
            PathSearchType type,
            double startX, double startY,
            double endX, double endY
    );

    /** 가장 최신(id 최대) Route 조회 */
    Optional<Route> findFirstByTypeAndStartXAndStartYAndEndXAndEndYOrderByIdDesc(
            PathSearchType type,
            double startX, double startY,
            double endX, double endY
    );
}
