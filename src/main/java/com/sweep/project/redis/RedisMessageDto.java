package com.sweep.project.redis;

import com.sweep.project.alarm.batch.AlarmType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RedisMessageDto {


    private String memberId;
    private String alarmId;
    private String token;
    private String alarmType;

    public RedisMessageDto(String memberId, String alarmId,String alarmType, String token) {
        this.memberId = memberId;
        this.alarmId = alarmId;
        this.token = token;
        this.alarmType = alarmType;
    }
}
