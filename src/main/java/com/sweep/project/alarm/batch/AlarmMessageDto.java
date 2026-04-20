package com.sweep.project.alarm.batch;

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
public class AlarmMessageDto {

    /** AMQP messageId 와 동일한 값 — 중복 방지 식별자 */
    private String messageId;

    private Long alarmId;
    private Long memberId;

    /** 해당 멤버의 FCM 토큰 목록 */
    private List<String> fcmTokens;

    /** 알람 유형 (PREPARE / DEPARTURE) */
    private AlarmType type;

    /** 이 알람이 실제로 울려야 하는 시각 */
    private LocalDateTime triggerTime;
}
