package com.sweep.project.route.bus;

import com.sweep.project.route.BoardingInfo;
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

    /** 최초 탑승 정류소 이름 */
    private String boardingStopName;

    /** 탑승할 버스 번호 */
    private String busNo;

    /**
     * 늦어도 이 시각에는 탑승해야 목적지 도착 희망 시각을 맞출 수 있음.
     * = desiredArrivalTime - (totalTime - timeToFirstStop)
     */
    private LocalTime latestBoardingTime;

    /**
     * 출발지에서 출발해야 하는 권장 시각.
     * = desiredArrivalTime - totalTime
     */
    private LocalTime recommendedDepartureTime;

    /**
     * 탑승 정류소에 곧 도착하는 버스 목록 (최대 2대, 도착 순).
     * getArrInfoByRoute API의 첫 번째·두 번째 버스 정보.
     */
    private List<ArrivingBus> arrivingBuses;

    @Data
    @AllArgsConstructor
    public static class ArrivingBus {
        /** 차량 번호판 */
        private String vehicleNo;
        /** 도착 예정 메시지 (예: "2분 30초 후 [2번째 전]") */
        private String arrivalMessage;
        /** 도착 예정 시간 (초) */
        private int arrivalTimeSeconds;
        /** 탑승 정류소까지 남은 정류소 수 */
        private int stopsRemaining;
        /** 현재 버스가 위치한 정류소명 */
        private String currentStation;
        /**
         * 혼잡도
         * 0: 정보없음, 3: 여유, 4: 보통, 5: 혼잡, 6: 매우혼잡
         */
        private int congestion;
        /** 막차 여부 */
        private boolean lastBus;
        /** GPS X 좌표 (경도) */
        private double gpsX;
        /** GPS Y 좌표 (위도) */
        private double gpsY;
    }
}
