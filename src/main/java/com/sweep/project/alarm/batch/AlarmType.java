package com.sweep.project.alarm.batch;

import io.swagger.v3.oas.annotations.media.Schema;

/** RabbitMQ 알람 메시지 유형 */
@Schema(description = "알람 유형 열거형")
public enum AlarmType {

    @Schema(description = "출발 준비 알람 — triggerTime = prepareStartTime + n × interval (n=0부터)")
    PREPARE,

    @Schema(description = "출발 알람 — triggerTime = arrivalTime - totalTime")
    DEPARTURE,

    @Schema(description = "경로 갱신 배치에서 소멸된 루트에 대해 사용자에게 재확인을 요청하는 알람")
    FIX
}
