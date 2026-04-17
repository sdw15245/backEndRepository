package com.sweep.project.alarm.repository;

import com.sweep.project.alarm.domain.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    List<Alarm> findAllByMemberIdAndDeletedFalse(Long memberId);
}