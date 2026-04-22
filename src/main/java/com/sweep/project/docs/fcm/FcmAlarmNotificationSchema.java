package com.sweep.project.docs.fcm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * RabbitMQ 컨슈머(RabbitMqManager)가 PREPARE / DEPARTURE 알람 수신 시
 * FCM으로 전송하는 Notification 메시지 구조 참고용 스키마.
 */
@Getter
@Schema(description = "PREPARE / DEPARTURE 알람 — FCM Notification 메시지")
public class FcmAlarmNotificationSchema {

    @Schema(description = "수신 대상 FCM 디바이스 토큰 (RedisMessageDto.token에서 추출)", example = "fcm-device-token-abc")
    private String token;

    @Schema(description = "FCM Notification 객체 — 시스템 트레이에 표시됨")
    private NotificationBlock notification;

    @Getter
    @Schema(description = "FCM Notification 블록")
    public static class NotificationBlock {

        @Schema(
            description = """
                알람 제목.
                현재는 'PREPARE'/'DEPARTURE' 모두 고정값 '테스팅'이며,
                AlarmType별 메시지로 전환 예정 (TODO in RabbitMqManager).
                """,
            example = "테스팅"
        )
        private String title;

        @Schema(
            description = """
                알람 본문.
                현재는 고정값 '테스팅'이며, AlarmType별 내용으로 전환 예정.
                - PREPARE  → '출발 N분 전입니다. 준비하세요.'
                - DEPARTURE → '지금 출발하세요!'
                """,
            example = "테스팅"
        )
        private String body;
    }
}
