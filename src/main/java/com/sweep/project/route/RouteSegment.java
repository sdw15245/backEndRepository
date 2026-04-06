package com.sweep.project.route;

/**
 * 경로 구간 공통 인터페이스
 * trafficType: 1 = 지하철, 2 = 버스, 3 = 도보
 */
public interface RouteSegment {
    int getTrafficType();
    int getSectionTime();
    int getDistance();
}
