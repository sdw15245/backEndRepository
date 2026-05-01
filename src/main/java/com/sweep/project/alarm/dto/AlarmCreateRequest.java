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
        @Min(1) @Max(1440)
        Integer prepareTime,

        @Schema(description = "준비 알람 발송 간격 (분), 기본값 20", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1) @Max(240)
        Integer interval,

        @Schema(description = "반복 알람 여부. true=매일/요일 반복, false=일회성", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean isLoop,

        @Schema(description = "반복 요일 (예: '월화수', 빈 문자열이면 매일). isLoop=true 시 필수", example = "월화수목금")
        @Pattern(regexp = "^[월화수목금토일]*$", message = "요일은 '월화수목금토일' 문자만 가능합니다.")
        @Size(max = 7)
        String day
) {
        @AssertTrue(message = "isLoop=true 일 때 prepareTime, interval, day 는 필수입니다.")
        public boolean isLoopFieldsValid() {
                if (Boolean.TRUE.equals(isLoop)) {
                        return prepareTime != null && interval != null && day != null;
                }
                return true;
        }
}
