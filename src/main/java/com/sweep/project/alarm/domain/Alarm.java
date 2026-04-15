package com.sweep.project.alarm.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alarmId;

    // 연관관계는 나중에 매핑 (지금은 ID만 저장)
    @Column(nullable = false)
    private Long routeTicketId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long routeId;

    private Integer interval;           // 준비 알림 간격 (분)
    private String day;                 // 반복 요일
    private Boolean isLoop;             // 반복 여부
    private LocalDateTime arrivalTime;  // 도착 시간
    private LocalDateTime startTime;    // 일정 날짜 (최초 알림 시점)
    private Boolean deleted = false;    // 삭제됐는지
    private Integer prepareTime;        // 사용자 지정 준비시간 (분)

    @Builder
    public Alarm(Long routeTicketId, Long memberId, Long routeId,
                 Integer interval, String day, Boolean isLoop,
                 LocalDateTime arrivalTime, LocalDateTime startTime,
                 Integer prepareTime) {
        this.routeTicketId = routeTicketId;
        this.memberId = memberId;
        this.routeId = routeId;
        this.interval = interval;
        this.day = day;
        this.isLoop = isLoop;
        this.arrivalTime = arrivalTime;
        this.startTime = startTime;
        this.prepareTime = prepareTime;
    }

    public void updateAlarm(LocalDateTime arrivalTime, LocalDateTime startTime,
                            Integer prepareTime, Boolean isLoop, String day) {
        this.arrivalTime = arrivalTime;
        this.startTime = startTime;
        this.prepareTime = prepareTime;
        this.isLoop = isLoop;
        this.day = day;
    }

    public void updateDeleted() {
        this.deleted = !this.deleted;
    }
}