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
    private Integer remainingMinutes;
    private Boolean prepareStart;
    private String checkList;

    public RedisMessageDto(String memberId, String alarmId, String alarmType, String token) {
        this(memberId, alarmId, alarmType, token, null, false, null);
    }

    public RedisMessageDto(String memberId, String alarmId, String alarmType, String token,
                           Integer remainingMinutes, Boolean prepareStart) {
        this(memberId, alarmId, alarmType, token, remainingMinutes, prepareStart, null);
    }

    public RedisMessageDto(String memberId, String alarmId, String alarmType, String token,
                           Integer remainingMinutes, Boolean prepareStart, String checkList) {
        this.memberId = memberId;
        this.alarmId = alarmId;
        this.token = token;
        this.alarmType = alarmType;
        this.remainingMinutes = remainingMinutes;
        this.prepareStart = prepareStart;
        this.checkList = checkList;
    }
}
