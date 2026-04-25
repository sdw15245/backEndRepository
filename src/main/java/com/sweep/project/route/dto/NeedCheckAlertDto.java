package com.sweep.project.route.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "경로 소멸 감지 시 FCM silent push에 사용되는 DTO — 클라이언트에 경로 재확인 요청")
public class NeedCheckAlertDto {

    @Schema(description = "재확인이 필요한 알람 ID", example = "5")
    private final Long alarmId;

    @Schema(description = "해당 멤버의 FCM 디바이스 토큰 목록")
    private final List<String> fcmTokens;

    public NeedCheckAlertDto(Long alarmId, List<String> fcmTokens) {
        this.alarmId = alarmId;
        this.fcmTokens = fcmTokens;
    }
}
