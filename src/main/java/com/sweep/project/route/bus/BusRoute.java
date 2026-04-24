package com.sweep.project.route.bus;

import com.sweep.project.route.RouteSegment;
import com.sweep.project.route.TrafficResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "버스 전용 경로 정보 (ODsay pathType=2)")
@Data
@AllArgsConstructor
public class BusRoute implements TrafficResponse {

    @Schema(description = "총 소요 시간 (분)", example = "38")
    private int totalTime;
    @Schema(description = "요금 (원)", example = "1500")
    private int payment;
    @Schema(description = "환승 횟수", example = "0")
    private int transferCount;
    @Schema(description = "버스 탑승 횟수", example = "1")
    private int busTransitCount;
    @Schema(description = "총 도보 거리 (미터)", example = "250")
    private int totalWalk;
    @Schema(description = "구간 목록. WalkSegment(trafficType=3)·BusSegment(trafficType=2) 순서 유지")
    private List<RouteSegment> segments;
    @Schema(description = "ODsay loadLane API의 mapObject 파라미터 값. 지도 폴리라인 조회 시 사용.만약 값에 0:0@ 이없어도 당황하지 말고 그대로 전달하시면됩니다"
            , example = "0:0@12018:1:5:22")
    private String mapObj;

    /**
     * 버스 구간 세부 정보 (trafficType = 2)
     */
    @Data
    @AllArgsConstructor
    public static class BusSegment implements RouteSegment {
        /** 버스 번호 (예: 6714) */
        private String busNo;
        /**
         * 버스 유형
         * 1: 일반버스, 2: 좌석버스, 3: 마을버스, 4: 직행좌석버스,
         * 5: 공항버스, 6: 간선급행버스, 10: 외곽버스, 11: 간선버스,
         * 12: 지선버스, 13: 순환버스, 14: 광역버스, 15: 급행간선버스
         */
        private int busType;
        /** 출발 정류장 이름 */
        private String startStop;
        /** 도착 정류장 이름 */
        private String endStop;
        /** 통과 정류장 수 */
        private int stationCount;
        /** 구간 소요 시간 (분) */
        private int sectionTime;
        /** 구간 거리 (미터) */
        private int distance;
        /**
         * 서울 버스 API busRouteId (ODsay lane.busID).
         * getArrInfoByRoute 호출 시 busRouteId 파라미터로 사용.
         */
        private int busRouteId;
        /**
         * 탑승 정류소 ID (ODsay subPath.startID).
         * getArrInfoByRoute 호출 시 stId 파라미터로 사용.
         */
        private int startStopId;
        /**
         * 탑승 정류소의 노선 내 순번 (ord).
         * ODsay에 해당 필드 없음 - getBusArrival 내부에서 BIS API로 자동 조회.
         */
        private int startStopOrder;

        /** 각 지역 출발 정류장 ID (BIS 제공지역인 경우에만 존재) */
        private String localBusStationId;
        /** 각 지역 출발 정류장 BIS 코드 (BIS 제공지역인 경우에만 존재) */
        private int stationProviderCode;
        /** 각 지역 버스노선 ID (BIS 제공지역인 경우에만 존재) */
        private String localBusId;
        /** 버스노선 BIS 코드 (BIS 제공지역인 경우에만 존재) */
        private int busProviderCode;

        @Override
        public int getTrafficType() {
            return 2;
        }
    }
}
