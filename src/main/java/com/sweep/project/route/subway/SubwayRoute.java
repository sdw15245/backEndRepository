package com.sweep.project.route.subway;

import com.sweep.project.route.RouteSegment;
import com.sweep.project.route.TrafficResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Schema(description = "지하철 전용 경로 정보 (ODsay pathType=1)")
@Data
@AllArgsConstructor
public class SubwayRoute implements TrafficResponse {

    @Schema(description = "총 소요 시간 (분)", example = "42")
    private int totalTime;
    @Schema(description = "요금 (원)", example = "1500")
    private int payment;
    @Schema(description = "환승 횟수", example = "1")
    private int transferCount;
    @Schema(description = "지하철 탑승 횟수", example = "2")
    private int subwayTransitCount;
    @Schema(description = "총 도보 거리 (미터)", example = "320")
    private int totalWalk;
    @Schema(description = "구간 목록. WalkSegment(trafficType=3)·SubwaySegment(trafficType=1) 순서 유지")
    private List<RouteSegment> segments;
    @Schema(description = "ODsay loadLane API의 mapObject 파라미터 값. 지도 폴리라인 조회 시 사용." +
            "만약 값에 0:0@ 이없어도 당황하지 말고 그대로 전달하시면됩니다", example = "0:0@3:2:310:329@2:2:200:215")
    private String mapObj;

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
