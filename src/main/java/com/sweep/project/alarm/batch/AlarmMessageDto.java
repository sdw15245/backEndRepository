package com.sweep.project.alarm.batch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RabbitMQ "alarm" 큐에 적재되는 메시지 DTO.
 *
 * <p>messageId 형식
 * <pre>
 *   준비 알람 : {alarmId}_{memberId}_prepare_{n}   (n = 0부터 시작)
 *   출발 알람 : {alarmId}_{memberId}_departure_0
 * </pre>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RabbitMQ 'alarm' 큐에 적재되는 FCM 알람 메시지 DTO")
public class AlarmMessageDto {

    @Schema(
        description = """
            AMQP messageId와 동일한 중복 방지 식별자.
            - 준비 알람: {alarmId}_{memberId}_prepare_{n}  (n=0부터)
            - 출발 알람: {alarmId}_{memberId}_departure_0
            """,
        example = "42_7_departure_0"
    )
    private String messageId;

    @Schema(description = "알람 ID", example = "42")
    private Long alarmId;

    @Schema(description = "멤버 ID", example = "7")
    private Long memberId;

    @Schema(description = "해당 멤버의 FCM 토큰 목록", example = "[\"fcm-token-abc\", \"fcm-token-xyz\"]")
    private List<String> fcmTokens;

    @Schema(description = "알람 유형 (PREPARE / DEPARTURE / FIX)")
    private AlarmType type;

    @Schema(description = "이 알람이 실제로 울려야 하는 시각", example = "2024-06-01T07:40:00")
    private LocalDateTime triggerTime;
}
