package com.sweep.project.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record AlarmCreateRequest(

        @Schema(description = "경로 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long routeId,

        @Schema(description = "알림 제목", example = "친구 약속", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 30)         //제목 현재 최대 30자
        String title,

        @Schema(description = "준비물", example = "우산")
        @Size(max = 200)        //내용 현재 최대 200자
        String checklist,

        @Schema(description = "목적지 도착 예정 시각 (ISO 8601)", example = "2024-06-01T09:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        LocalDateTime arrivalTime,

        @Schema(description = "알람 최초 발생 기준 시각. 오늘 날짜이면 남은 트리거를 즉시 Redis 에 등록", example = "2024-06-01T07:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        LocalDateTime startTime,

        @Schema(description = "출발 전 준비 시간 (분), 기본값 60", example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(0) @Max(1440)
        Integer prepareTime,

        @Schema(description = "준비 알람 발송 간격 (분), 기본값 20", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(0) @Max(240)
        Integer interval,

        @Schema(description = "출발지 명칭",requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String startName,

        @Schema(description = "도착지 명칭", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String endName
) {

}


