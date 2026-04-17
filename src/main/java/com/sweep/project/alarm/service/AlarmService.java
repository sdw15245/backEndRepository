package com.sweep.project.alarm.service;

import com.sweep.project.alarm.domain.Alarm;
import com.sweep.project.alarm.dto.AlarmCreateRequest;
import com.sweep.project.alarm.dto.AlarmUpdateRequest;
import com.sweep.project.alarm.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlarmService {

    private final AlarmRepository alarmRepository;

    public Alarm createAlarm(Long memberId, AlarmCreateRequest req) {
        Alarm alarm = Alarm.builder()
                .memberId(memberId)
                .routeTicketId(req.routeTicketId())
                .routeId(req.routeId())
                .arrivalTime(req.arrivalTime())
                .startTime(req.startTime())
                .prepareTime(req.prepareTime())
                .interval(req.interval())
                .isLoop(req.isLoop())
                .day(req.day())
                .build();
        return alarmRepository.save(alarm);
    }

    public List<Alarm> getMyAlarms(Long memberId) {
        return alarmRepository.findAllByMemberIdAndDeletedFalse(memberId);
    }

    public void updateAlarm(Long alarmId, AlarmUpdateRequest req) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        alarm.updateAlarm(req.arrivalTime(), req.startTime(),
                req.prepareTime(), req.interval(), req.isLoop(), req.day());
    }

    public void deleteAlarm(Long alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        alarm.updateDeleted();
    }
}
