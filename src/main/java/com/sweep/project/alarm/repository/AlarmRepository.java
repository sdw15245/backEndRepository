package com.sweep.project.alarm.repository;

import com.sweep.project.alarm.domain.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    List<Alarm> findAllByRouteTicket_Member_IdAndDeletedFalse(Long memberId);

    List<Alarm> findAllByRouteTicket_IdAndDeletedFalse(Long routeTicketId);
}
