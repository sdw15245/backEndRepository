package com.sweep.project.route.subway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * ODsay searchSubwaySchedule API 응답 원본 DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubwayScheduleResponse {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String stationName;
        private int stationID;
        private String laneName;
        private WeekSchedule weekdaySchedule;
        private WeekSchedule saturdaySchedule;
        private WeekSchedule holidaySchedule;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeekSchedule {
        /** 상행 열차 시간표 */
        private List<TrainTime> up;
        /** 하행 열차 시간표 */
        private List<TrainTime> down;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrainTime {
        /** 출발 시각 (HH:mm:ss) */
        private String departureTime;
        /** 종착역 이름 */
        private String endStationName;
        /** 열차 종류 (0: 일반, 1: 급행, 2: 특급) */
        private int subwayClass;
        /** 첫차/막차 여부 (0: 일반, 1: 첫차, 2: 막차, 3: 첫차+막차) */
        private int firstLastFlag;
    }
}
