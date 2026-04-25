package com.sweep.project.route.subway;

import com.sweep.project.route.RouteSegment;
import com.sweep.project.route.TrafficResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Schema(description = "지하철 전용 경로 정보 (ODsay pathType=1)")
@Data
public class SubwayRoute implements TrafficResponse {

    @Schema(description = "DB Route ID", example = "10")
    private Long routeId;

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

    public SubwayRoute(int totalTime, int payment, int transferCount,
                       int subwayTransitCount, int totalWalk,
                       List<RouteSegment> segments, String mapObj) {
        this.totalTime = totalTime;
        this.payment = payment;
        this.transferCount = transferCount;
        this.subwayTransitCount = subwayTransitCount;
        this.totalWalk = totalWalk;
        this.segments = segments;
        this.mapObj = mapObj;
    }

    @Schema(description = "지하철 탑승 구간 세부 정보 (trafficType = 1)")
    @Data
    @AllArgsConstructor
    public static class SubwaySegment implements RouteSegment {

        @Schema(description = "노선명", example = "수도권 3호선")
        private String lineName;

        @Schema(description = "지하철 노선 코드 (ODsay lane.subwayCode). loadLane mapObject 구성 시 ID로 사용", example = "3")
        private int subwayCode;

        @Schema(description = "탑승역 이름", example = "교대")
        private String startStation;

        @Schema(description = "하차역 이름", example = "을지로3가")
        private String endStation;

        @Schema(description = "탑승~하차 구간 역 수", example = "7")
        private int stationCount;

        @Schema(description = "구간 소요 시간 (분)", example = "14")
        private int sectionTime;

        @Schema(description = "구간 거리 (미터)", example = "9200")
        private int distance;

        @Schema(description = "탑승역 ID (ODsay subPath.startID). searchSubwaySchedule API의 stationID 파라미터로 사용", example = "310")
        private int startID;

        @Schema(description = "하차역 ID (ODsay subPath.endID). loadLane mapObject 구성 시 EndIdx로 사용", example = "329")
        private int endId;

        @Schema(description = "운행 방향 코드. 1=상행(up 시간표), 2=하행(down 시간표)", example = "2")
        private int wayCode;

        @Override
        public int getTrafficType() {
            return 1;
        }
    }
}
