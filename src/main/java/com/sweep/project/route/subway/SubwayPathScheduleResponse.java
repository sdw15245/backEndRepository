package com.sweep.project.route.subway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * ODsay subwayPathSchedule API 응답 DTO.
 * <p>
 * 실제 응답 기준:
 * <ul>
 *   <li>구간 타입: movingType 1=지하철, 2=환승도보</li>
 *   <li>시각 형식: "HH:mm:ss"</li>
 *   <li>노선명: lane 배열 없이 laneName 직접 필드</li>
 * </ul>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubwayPathScheduleResponse {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        /** 0=정상, 2=첫차 대체, 3=막차 대체 */
        private int notificationCode;
        private String notificationMessage;
        private List<Path> path;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        /** 1=최단 시간, 2=최소 환승 */
        private int pathType;
        private Info info;
        private List<SubPath> subPath;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private int day;
        /** 총 소요 시간 (분) */
        private int totalTime;
        private int subwayTravelTime;
        private int exchangeWalkTime;
        private int subwayTravelDistance;
        private String firstStartStationName;
        private int firstStartLaneID;
        private String firstStartLaneName;
        private String lastEndStationName;
        private int lastEndLaneID;
        private String lastEndLaneName;
        /** 출발 시각 (HH:mm:ss) */
        private String departureTime;
        /** 최종 도착 시각 (HH:mm:ss) */
        private String arrivalTime;
        private int stationCount;
        private int transferCount;
        private int cardFare;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubPath {
        /** 1=지하철, 2=환승도보 */
        private int movingType;
        /** 구간 소요 시간 (분) */
        private int sectionTime;
        private int stopStationCount;
        private String startName;
        private String endName;
        private int startID;
        private int endID;
        private int wayCode;
        /** 노선명 (예: "3호선") */
        private String laneName;
        private int laneID;
        private String isExpressLane;
        /** 이 구간 출발역 출발 시각 (HH:mm:ss) – 탑승 시각으로 사용 */
        private String departureTime;
        /** 이 구간 도착역 도착 시각 (HH:mm:ss) */
        private String arrivalTime;
        /** 행선지 방향명 */
        private String wayName;
        /** 직전 열차 정보 (해당 출발역 기준) */
        private PrevNextTrain prevTrain;
        /** 다음 열차 정보 (해당 출발역 기준) */
        private PrevNextTrain nextTrain;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrevNextTrain {
        private int laneID;
        private String laneName;
        private String isExpressLane;
        /** 해당 역 출발 시각 (HH:mm:ss) */
        private String departureTime;
        private String wayName;
    }
}
