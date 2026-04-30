package com.sweep.project.fcm.dto;

import com.sweep.project.alarm.batch.AlarmType;
import com.sweep.project.fcm.domain.FcmSendLog;

import java.time.LocalDateTime;

// 클라이언트에 내려줄 응답 DTO
public record FcmSendLogResponse(
        Long id,
        Long alarmId,
        AlarmType alarmType,
        String title,
        String body,
        LocalDateTime sentAt
) {

    public FcmSendLogResponse(FcmSendLog log) {
        this(
                log.getId(),
                log.getAlarmId(),
                log.getAlarmType(),
                log.getTitle(),
                log.getBody(),
                log.getSentAt()
        );
    }
}
