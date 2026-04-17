package com.sweep.project.route.bus;

import com.sweep.project.route.BoardingInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * 버스 탑승 정보.
 * 목적지 도착 희망 시각을 기반으로 최초 탑승 정류소에 곧 도착하는 버스 2대의
 * 실시간 도착 예정 정보와 출발 권장 시각을 담는다.
 */
@Data
@AllArgsConstructor
public class BusBoardingInfo implements BoardingInfo {


    @Schema(description = "최초 탑승 버스 정류소의 이름")
    /** 최초 탑승 정류소 이름 */
    private String boardingStopName;

    @Schema(description = "탑승할 버스의 노선 번호")
    /** 탑승할 버스 번호 */
    private String busNo;


    @Schema(description = "늦어도 이시각에는 대중교통을 탑승해야 목적지 도착 희망시간을 맞출수있음")
    /**
     * 늦어도 이 시각에는 탑승해야 목적지 도착 희망 시각을 맞출 수 있음.
     * = desiredArrivalTime - (totalTime - timeToFirstStop)
     */
    private LocalTime latestBoardingTime;

    @Schema(description = "출발지에서 첫번쨰 탑승 교동 구간까지 출발해야되는 권장 시간")
    /**
     * 출발지에서 출발해야 하는 권장 시각.
     * = desiredArrivalTime - totalTime
     */
    private LocalTime recommendedDepartureTime;

    @Schema(description = "탑승 정류소에곧 도착하는 버스목록")
    /**
     * 탑승 정류소에 곧 도착하는 버스 목록 (최대 2대, 도착 순).
     * getArrInfoByRoute API의 첫 번째·두 번째 버스 정보.
     */
    private List<ArrivingBus> arrivingBuses;

    @Data
    @AllArgsConstructor
    public static class ArrivingBus {
        @Schema(description = "차량 번호판", example = "서울 70 가 1234")
        private String vehicleNo;
        @Schema(description = "도착 예정 메시지",example = "2분 30초후")
        /** 도착 예정 메시지 (예: "2분 30초 후 [2번째 전]") */
        private String arrivalMessage;
        @Schema(description = "초단위 도착 예정 시간")
        /** 도착 예정 시간 (초) */
        private int arrivalTimeSeconds;

        @Schema(description = "탑승 정류소까지 남은 정류소")
        /** 탑승 정류소까지 남은 정류소 수 */
        private int stopsRemaining;
        @Schema(description = "햔재 버스가 위치한 정류소명")
        /** 현재 버스가 위치한 정류소명 */
        private String currentStation;
        @Schema(description = "혼잡도",example ="0: 정보없음, 3: 여유, 4: 보통, 5: 혼잡, 6: 매우혼잡" )
        /**
         * 혼잡도
         * 0: 정보없음, 3: 여유, 4: 보통, 5: 혼잡, 6: 매우혼잡
         */
        private int congestion;
        @Schema(description = "막차여부")
        /** 막차 여부 */
        private boolean lastBus;
        @Schema(description = "버스 현재 위치 GPS X 좌표 (경도)", example = "126.9780")
        private double gpsX;
        @Schema(description = "버스 현재 위치 GPS Y 좌표 (위도)", example = "37.5665")
        private double gpsY;
    }
}
