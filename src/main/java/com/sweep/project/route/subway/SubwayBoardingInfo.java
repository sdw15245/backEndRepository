package com.sweep.project.route.subway;

import com.sweep.project.route.BoardingInfo;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "지하철 탑승 정보")
public class SubwayBoardingInfo implements BoardingInfo {

    @Schema(description = "최초 탑승역 이름", example = "강남")
    private String boardingStationName;

    @Schema(description = "탑승 노선명", example = "2호선")
    private String lineName;

    @Schema(description = "늦어도 이 시각에는 탑승해야 목적지 도착 희망 시각을 맞출 수 있음", example = "08:45:00")
    private LocalTime latestBoardingTime;

    @Schema(description = "출발지에서 출발해야 하는 권장 시각", example = "08:30:00")
    private LocalTime recommendedDepartureTime;

    @Schema(description = "latestBoardingTime 이전 출발 열차 목록 (가장 가까운 순, 최대 3편성)")
    private List<TrainSchedule> availableTrains;

    @Data
    @AllArgsConstructor
    @Schema(description = "탑승 가능 열차 정보")
    public static class TrainSchedule {

        @Schema(description = "해당 역 출발 시각", example = "08:42:00")
        private LocalTime departureTime;

        @Schema(description = "종착역 이름", example = "성수")
        private String endStationName;

        @Schema(description = "열차 종류. 0: 일반, 1: 급행, 2: 특급", example = "0")
        private int subwayClass;

        @Schema(description = "첫차/막차 여부. 0: 일반, 1: 첫차, 2: 막차, 3: 첫차+막차", example = "0")
        private int firstLastFlag;
    }
}
