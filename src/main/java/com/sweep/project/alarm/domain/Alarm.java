package com.sweep.project.alarm.domain;

import com.sweep.project.member.domain.Member;
import com.sweep.project.route.domain.Route;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    private Boolean needCheck = false;
    private LocalDateTime createdAt;

    private Integer interval;           // 준비 알림 간격 (분)
    private String day;                 // 반복 요일
    private Boolean isLoop;             // 반복 여부
    private LocalDateTime arrivalTime;  // 도착 시간
    private LocalDateTime startTime;    // 일정 날짜 (최초 알림 시점)
    private Boolean deleted = false;
    private Integer prepareTime;        // 사용자 지정 준비시간 (분)

    @Builder
    public Alarm(Member member, Route route,
                 Integer interval, String day, Boolean isLoop,
                 LocalDateTime arrivalTime, LocalDateTime startTime,
                 Integer prepareTime) {
        this.member = member;
        this.route = route;
        this.interval = interval;
        this.day = day;
        this.isLoop = isLoop;
        this.arrivalTime = arrivalTime;
        this.startTime = startTime;
        this.prepareTime = prepareTime;
        this.createdAt = LocalDateTime.now();
    }

    // ── 편의 접근자 ────────────────────────────────────────────────────────────

    public Long getMemberId() { return member.getId(); }
    public Long getRouteId()  { return route.getId(); }

    // ── 변경 메서드 ────────────────────────────────────────────────────────────

    public void updateAlarm(Route route, LocalDateTime arrivalTime, LocalDateTime startTime,
                            Integer prepareTime, Integer interval,
                            Boolean isLoop, String day) {
        this.route = route;
        this.arrivalTime = arrivalTime;
        this.startTime = startTime;
        this.prepareTime = prepareTime;
        this.interval = interval;
        this.isLoop = isLoop;
        this.day = day;
    }

    public void updateDeleted() {
        this.deleted = !this.deleted;
    }

    public void updateNeedCheck() {
        this.needCheck = !this.needCheck;
    }
}
