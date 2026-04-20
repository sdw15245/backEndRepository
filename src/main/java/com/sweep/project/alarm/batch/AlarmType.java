package com.sweep.project.alarm.batch;

/** RabbitMQ 알람 메시지 유형 */
public enum AlarmType {
    /** 출발 준비 알람 (prepareStartTime + n * interval) */
    PREPARE,
    /** 출발 알람 (arrivalTime - totalTime) */
    DEPARTURE,
    /** route가 갱신되서 폐쇠된 케이스 즉 주시적 rotue업데이트 과정에서 사라진 루트에 대해서 전송하는 타입*/
    FIX
}
