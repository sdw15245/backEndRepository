package com.sweep.project.route.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteTicketRepository extends JpaRepository<RouteTicket, Long> {

    /** 특정 멤버의 티켓 + Route 를 한 번에 조회 (N+1 방지) */
    @Query("SELECT rt FROM RouteTicket rt JOIN FETCH rt.route WHERE rt.member.id = :memberId")
    List<RouteTicket> findByMemberIdWithRoute(@Param("memberId") Long memberId);

    /** 특정 멤버 + 특정 경로의 티켓 존재 여부 */
    boolean existsByMemberIdAndRouteId(Long memberId, Long routeId);
}
