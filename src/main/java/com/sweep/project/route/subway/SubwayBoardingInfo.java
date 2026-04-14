package com.sweep.project.route.subway;

import com.sweep.project.route.BoardingInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * 지하철 탑승 정보
 * - 목적지 도착 희망 시각을 기반으로 최초 탑승역의 탑승 가능 열차 및 출발 권장 시각을 담는다.
 */
@Data
@AllArgsConstructor
public class SubwayBoardingInfo implements BoardingInfo {

    /** 최초 탑승역 이름 */
    private String boardingStationName;

    /** 노선명 */
    private String lineName;

    /**
     * 늦어도 이 시각에는 탑승해야 목적지 도착 희망 시각을 맞출 수 있음.
     * = desiredArrivalTime - (totalTime - timeToFirstStation)
     */
    private LocalTime latestBoardingTime;

    /**
     * 출발지에서 출발해야 하는 권장 시각.
     * = desiredArrivalTime - totalTime
     */
    private LocalTime recommendedDepartureTime;

    /**
     * latestBoardingTime 이전에 출발하는 열차 목록 (가장 가까운 순으로 최대 3편성).
     * 이 중 탑승 가능한 열차를 선택하면 된다.
     */
    private List<TrainSchedule> availableTrains;

    @Data
    @AllArgsConstructor
    public static class TrainSchedule {
        /** 해당 역 출발 시각 */
        private LocalTime departureTime;
        /** 종착역 이름 */
        private String endStationName;
        /**
         * 열차 종류
         * 0: 일반, 1: 급행, 2: 특급
         */
        private int subwayClass;
        /**
         * 첫차/막차 여부
         * 0: 일반, 1: 첫차, 2: 막차, 3: 첫차+막차
         */
        private int firstLastFlag;
    }
}
