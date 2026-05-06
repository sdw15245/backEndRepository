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
    private Integer remainingMinutes;       // ex) remainingMinutes == 20이라면 "20분 후에 출발해야 해요!"
    private Boolean prepareStart;           // ex) prepareStart == true 라면 "지금 준비 시작해야 해요!"

    public RedisMessageDto(String memberId, String alarmId,String alarmType, String token) {
        this(memberId, alarmId, alarmType, token, null, false);
    }

    public RedisMessageDto(String memberId, String alarmId,String alarmType, String token, Integer remainingMinutes) {
        this(memberId, alarmId, alarmType, token, remainingMinutes, false);
    }

    public RedisMessageDto(String memberId, String alarmId,String alarmType, String token,
                           Integer remainingMinutes, Boolean prepareStart) {
        this.memberId = memberId;
        this.alarmId = alarmId;
        this.token = token;
        this.alarmType = alarmType;
        this.remainingMinutes = remainingMinutes;
        this.prepareStart = prepareStart;
    }
}
