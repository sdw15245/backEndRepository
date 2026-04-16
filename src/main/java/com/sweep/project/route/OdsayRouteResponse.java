package com.sweep.project.route;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * ODsay searchPubTransPathT API 응답 원본 DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OdsayRouteResponse {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        /** 검색 결과 분류 (0: 도시내, 1: 도시간직통, 2: 도시간환승) */
        private int searchType;
        private int outTrafficCheck;
        private int busCount;
        private int subwayCount;
        private int trafficCount;
        private List<Path> path;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        /**
         * 경로 유형
         * 1: 지하철, 2: 버스, 3: 버스+지하철 혼합
         */
        private int pathType;
        private Info info;
        private List<SubPath> subPath;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        /** 총 소요 시간 (분) */
        private int totalTime;
        /** 총 도보 거리 (미터) */
        private int totalWalk;
        /** 총 이동 거리 (미터) */
        private int trafficDistance;
        /** 요금 (원) */
        private int payment;
        /** 버스 탑승 횟수 */
        private int busTransitCount;
        /** 지하철 탑승 횟수 */
        private int subwayTransitCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubPath {
        /**
         * 이동 수단 유형
         * 1: 지하철, 2: 버스, 3: 도보
         */
        private int trafficType;
        /** 구간 거리 (미터) */
        private int distance;
        /** 구간 소요 시간 (분) */
        private int sectionTime;
        /** 통과 정거장 수 */
        private int stationCount;
        /** 출발 정류장/역 이름 */
        private String startName;
        /** 도착 정류장/역 이름 */
        private String endName;
        /** 출발 역/정류소 ID (지하철: searchSubwaySchedule stationID, 버스: 서울 버스 API arsId) */
        private int startID;
        /** 도착 역/정류소 ID */
        private int endID;
        /** 방향 코드 (1: 상행, 2: 하행) */
        private int wayCode;
        // startExNo: ODsay 스펙에 없는 필드 - ord는 각 BIS API에서 별도 조회
        /** 출발 버스 정류소의 local id값 - 공공데이터 포털에서 제공하는 서울시 버스 정류장 ID 값 */
        private String startLocalStationID;
        /** 각 지역 출발 정류장 고유번호 (BIS 제공지역인 경우에만 존재) */
        private String startArsID;
        /** 출발 정류장 BIS 코드 (BIS 제공지역인 경우에만 존재) */
        private int startStationProviderCode;
        /** 출발 X 좌표 */
        private BigDecimal startX;
        /** 출발 Y 좌표 */
        private BigDecimal startY;
        /** 도착 X 좌표 */
        private BigDecimal endX;
        /** 도착 Y 좌표 */
        private BigDecimal endY;
        /** 노선 정보 목록 (지하철/버스 구간에만 존재) */
        private List<Lane> lane;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Lane {
        // 지하철 노선 필드
        /** 지하철 노선명 (예: 수도권 2호선) */
        private String name;
        /** 지하철 노선 코드 */
        private int subwayCode;
        /** 지하철 도시 코드 */
        private int subwayCityCode;

        // 버스 노선 필드
        /** 버스 번호 (예: 6714) */
        private String busNo;
        /**
         * 버스 유형
         * 1: 일반버스, 2: 좌석버스, 3: 마을버스, 4: 직행좌석버스,
         * 5: 공항버스, 6: 간선급행버스, 10: 외곽버스, 11: 간선버스,
         * 12: 지선버스, 13: 순환버스, 14: 광역버스, 15: 급행간선버스
         */
        private int type;
        /** 버스 고유 ID */
        private int busID;
        /**
         * 공공데이터 포털에서 제공하는 서울시 버스 ID 값.
         */
        private String busLocalBlID;
        /** BIS 코드 (BIS 제공지역인 경우에만 존재) */
        private int busProviderCode;
    }
}
