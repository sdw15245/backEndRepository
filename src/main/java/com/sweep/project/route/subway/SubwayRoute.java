package com.sweep.project.route.subway;

import com.sweep.project.route.RouteSegment;
import com.sweep.project.route.TrafficResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * ODsay API 응답에서 파싱된 지하철 경로 정보
 */
@Data
@AllArgsConstructor
public class SubwayRoute implements TrafficResponse {

    /** 총 소요 시간 (분) */
    private int totalTime;
    /** 요금 (원) */
    private int payment;
    /** 환승 횟수 */
    private int transferCount;
    /** 지하철 탑승 횟수 */
    private int subwayTransitCount;
    /** 총 도보 거리 (미터) */
    private int totalWalk;
    /** 구간 목록 (지하철 구간 + 도보 구간, 응답 순서 유지) */
    private List<RouteSegment> segments;

    /**
     * 지하철 구간 세부 정보 (trafficType = 1)
     */
    @Data
    @AllArgsConstructor
    public static class SubwaySegment implements RouteSegment {
        /** 노선명 (예: 수도권 2호선) */
        private String lineName;
        /** 지하철 노선 코드 */
        private int subwayCode;
        /** 출발역 이름 */
        private String startStation;
        /** 도착역 이름 */
        private String endStation;
        /** 통과 역 수 */
        private int stationCount;
        /** 구간 소요 시간 (분) */
        private int sectionTime;
        /** 구간 거리 (미터) */
        private int distance;
        /** 출발역 ID (searchSubwaySchedule API의 stationID로 사용) */
        private int startID;
        /** 도착역 id(구간 종착)*/
        private int endId;
        /** 방향 코드 (1: 상행 → up 스케줄, 2: 하행 → down 스케줄) */
        private int wayCode;

        @Override
        public int getTrafficType() {
            return 1;
        }
    }
}
