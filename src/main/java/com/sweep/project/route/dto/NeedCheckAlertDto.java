package com.sweep.project.route.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "경로 소멸 감지 시 FCM silent push에 사용되는 DTO — 클라이언트에 경로 재확인 요청")
public class NeedCheckAlertDto {

    @Schema(description = "FCM 디바이스 토큰", example = "fcm-token-abc123")
    private String fcmToken;

    @Schema(description = "재확인이 필요한 알람 ID", example = "5")
    private Long alarmId;

    public NeedCheckAlertDto(String fcmToken, Long alarmId) {
        this.fcmToken = fcmToken;
        this.alarmId=alarmId;
    }
}
