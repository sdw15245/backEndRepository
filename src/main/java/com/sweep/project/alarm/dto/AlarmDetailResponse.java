package com.sweep.project.alarm.dto;

import com.sweep.project.alarm.domain.Alarm;
import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.domain.Route;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "알람 상세 응답 - 알람 정보 및 연결된 경로 정보 포함")
public class AlarmDetailResponse {

    // ── 알람 정보 ────────────────────────────────────────────────────────────

    @Schema(description = "알람 ID", example = "1")
    private final Long alarmId;

    @Schema(description = "알림 제목", example = "친구약속")
    private final String title;

    @Schema(description = "준비물", example = "우산")
    private final String checklist;

    @Schema(description = "알람 최초 발생 기준 시각 (출발 기준)", example = "2024-06-01T07:00:00")
    private final LocalDateTime startTime;

    @Schema(description = "목적지 도착 예정 시각", example = "2024-06-01T09:00:00")
    private final LocalDateTime arrivalTime;

    @Schema(description = "준비 알람 발송 간격 (분)", example = "20")
    private final Integer interval;

    @Schema(description = "출발 전 준비 시간 (분)", example = "60")
    private final Integer prepareTime;

    @Schema(description = "기존의 루트에 변경 사항이있어서 루트에 대한 새로이 검증이 필요하다는값. true면 검증이 필요하다.", example = "false")
    private final Boolean needCheck;

    @Schema(description = "알람 생성 시각", example = "2024-05-20T12:00:00")
    private final LocalDateTime createdAt;

    @Schema(description = "출발지명")
    private final String startName;

    @Schema(description = "도착지명")
    private final String endName;

    @Schema(description = "전철의 경우 실질적으로 걸리는 시간")
    private final Integer actualTime;

    // ── Route 정보 ───────────────────────────────────────────────────────────

    @Schema(description = "경로 ID", example = "10")
    private final Long routeId;

    @Schema(description = "경로 탐색 유형 (PATH_TYPE_ANYONE / PATH_TYPE_SUBWAY / PATH_TYPE_BUS)",
            example = "PATH_TYPE_SUBWAY")
    private final PathSearchType routeType;

    @Schema(description = "출발지 경도", example = "126.9769")
    private final double startX;

    @Schema(description = "출발지 위도", example = "37.5796")
    private final double startY;

    @Schema(description = "목적지 경도", example = "127.0276")
    private final double endX;

    @Schema(description = "목적지 위도", example = "37.4979")
    private final double endY;

    @Schema(description = "총 소요 시간 (분)", example = "45")
    private final Integer totalTime;

    @Schema(description = "ODsay API 단일 경로 JSON (TrafficResponse 직렬화 원문)")
    private final String routeData;



    public AlarmDetailResponse(Alarm alarm) {
        this.alarmId     = alarm.getAlarmId();
        this.title       = alarm.getTitle();
        this.checklist   = alarm.getChecklist();
        this.startTime   = alarm.getStartTime();
        this.arrivalTime = alarm.getArrivalTime();
        this.interval    = alarm.getInterval();
        this.prepareTime = alarm.getPrepareTime();
        this.needCheck   = alarm.getNeedCheck();
        this.createdAt   = alarm.getCreatedAt();
        this.startName=alarm.getStartName();
        this.endName=alarm.getEndName();
        this.actualTime=alarm.getActualTime();

        Route route = alarm.getRoute();
        this.routeId=route.getId();
        this.routeType = route.getType();
        this.startX    = route.getStartX();
        this.startY    = route.getStartY();
        this.endX      = route.getEndX();
        this.endY      = route.getEndY();
        this.totalTime = route.getTotalTime();
        this.routeData = route.getRouteData();
    }
}
