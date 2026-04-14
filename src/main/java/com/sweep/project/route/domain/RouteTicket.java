package com.sweep.project.route.domain;

import com.sweep.project.member.domain.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Member 와 Route 를 연결하는 티켓 엔티티.
 * <p>
 * 하나의 Member 는 여러 RouteTicket 을 보유할 수 있고,
 * 하나의 Route 는 여러 RouteTicket 에서 참조될 수 있다.
 */
@Entity
@Table(name = "route_ticket")
@Getter
@NoArgsConstructor
public class RouteTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    private LocalDateTime createdAt;

    @Builder
    public RouteTicket(Member member, Route route) {
        this.member = member;
        this.route = route;
        this.createdAt = LocalDateTime.now();
    }
}
