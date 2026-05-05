package com.sweep.project.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AlarmListResponse {

    @Schema(description = "현재 시간 기준으로 가장 처음에 울릴 알람에대한 상세정보")
    private AlarmDetailResponse alarmDetailResponse;
    @Schema(description = "그외에 시간을 오름차순으로 정렬한 기타 알람들")
    private List<AlarmSummaryResponse> alarmSummaryResponseList;

    public AlarmListResponse(AlarmDetailResponse alarmDetailResponse, List<AlarmSummaryResponse> alarmSummaryResponseList) {
        this.alarmDetailResponse = alarmDetailResponse;
        this.alarmSummaryResponseList = alarmSummaryResponseList;
    }
}
