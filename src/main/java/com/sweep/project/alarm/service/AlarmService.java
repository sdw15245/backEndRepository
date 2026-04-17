package com.sweep.project.alarm.service;

import com.sweep.project.alarm.domain.Alarm;
import com.sweep.project.alarm.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlarmService {

    private final AlarmRepository alarmRepository;

    // 생성
    public Alarm createAlarm(Long memberId, Long routeTicketId, Long routeId,
                             LocalDateTime arrivalTime, LocalDateTime startTime,
                             Integer prepareTime, Integer interval,
                             Boolean isLoop, String day) {
        Alarm alarm = Alarm.builder()
                .memberId(memberId)
                .routeTicketId(routeTicketId)
                .routeId(routeId)
                .arrivalTime(arrivalTime)
                .startTime(startTime)
                .prepareTime(prepareTime)
                .interval(interval)
                .isLoop(isLoop)
                .day(day)
                .build();
        return alarmRepository.save(alarm);
    }

    // 조회 (내 알림 목록)
    public List<Alarm> getMyAlarms(Long memberId) {
        return alarmRepository.findAllByMemberIdAndDeletedFalse(memberId);
    }

    // 수정
    public void updateAlarm(Long alarmId, LocalDateTime arrivalTime,
                            LocalDateTime startTime, Integer prepareTime,
                            Boolean isLoop, String day) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        alarm.updateAlarm(arrivalTime, startTime, prepareTime, isLoop, day);
    }

    // 삭제
    public void deleteAlarm(Long alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        alarm.updateDeleted();
    }
}