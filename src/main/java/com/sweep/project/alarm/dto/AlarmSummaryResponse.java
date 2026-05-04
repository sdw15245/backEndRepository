package com.sweep.project.alarm.dto;

import com.sweep.project.alarm.domain.Alarm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "알람 목록 응답 - 요약 정보")
public class AlarmSummaryResponse {

    @Schema(description = "알람 ID", example = "1")
    private final Long alarmId;

    @Schema(description = "알림 제목", example = "친구 약속")
    private final String title;

    @Schema(description = "준비물", example = "우산")
    private final String checklist;

    @Schema(description = "알람 최초 발생 기준 시각 (출발 기준)", example = "2024-06-01T07:00:00")
    private final LocalDateTime startTime;

    @Schema(description = "목적지 도착 예정 시각", example = "2024-06-01T09:00:00")
    private final LocalDateTime arrivalTime;

    public AlarmSummaryResponse(Alarm alarm) {
        this.alarmId     = alarm.getAlarmId();
        this.title       = alarm.getTitle();
        this.checklist   = alarm.getChecklist();
        this.startTime   = alarm.getStartTime();
        this.arrivalTime = alarm.getArrivalTime();
    }
}
