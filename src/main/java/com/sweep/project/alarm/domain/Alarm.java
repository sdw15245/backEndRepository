package com.sweep.project.alarm.domain;

import com.sweep.project.route.domain.RouteTicket;
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
    @JoinColumn(name = "route_ticket_id", nullable = false)
    private RouteTicket routeTicket;

    private Integer interval;           // 준비 알림 간격 (분)
    private String day;                 // 반복 요일
    private Boolean isLoop;             // 반복 여부
    private LocalDateTime arrivalTime;  // 도착 시간
    private LocalDateTime startTime;    // 일정 날짜 (최초 알림 시점)
    private Boolean deleted = false;    // 삭제됐는지
    private Integer prepareTime;        // 사용자 지정 준비시간 (분)

    @Builder
    public Alarm(RouteTicket routeTicket,
                 Integer interval, String day, Boolean isLoop,
                 LocalDateTime arrivalTime, LocalDateTime startTime,
                 Integer prepareTime) {
        this.routeTicket = routeTicket;
        this.interval = interval;
        this.day = day;
        this.isLoop = isLoop;
        this.arrivalTime = arrivalTime;
        this.startTime = startTime;
        this.prepareTime = prepareTime;
    }

    // ── 편의 접근자 ────────────────────────────────────────────────────────────

    public Long getRouteTicketId() { return routeTicket.getId(); }
    public Long getMemberId()      { return routeTicket.getMember().getId(); }
    public Long getRouteId()       { return routeTicket.getRoute().getId(); }

    // ── 변경 메서드 ────────────────────────────────────────────────────────────

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
