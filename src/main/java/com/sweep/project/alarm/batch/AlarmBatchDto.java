package com.sweep.project.alarm.batch;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * AlarmZeroOffsetReader 출력 DTO.
 * Alarm + Route 조인 결과를 담는다.
 */
@Getter
@AllArgsConstructor
public class AlarmBatchDto {

    /** alarm.alarmId — zero-offset cursor 기준 */
    private Long alarmId;

    private Long memberId;

    /** 사용자 지정 준비 시간 (분), null 이면 준비 알람 없음 */
    private Integer prepareTime;

    /** 준비 알람 간격 (분), null 이면 준비 알람 없음 */
    private Integer interval;

    /** 목적지 도착 예정 시각 */
    private LocalDateTime arrivalTime;

    /**
     * 반복 요일 문자열 (예: "월화수", "월,화,수").
     * null 이거나 빈 문자열이면 매일 울린다고 간주한다.
     */
    private String day;

    /** Route.totalTime (분) — null 이면 소요시간 불명으로 스킵 */
    private Integer totalTime;
}
