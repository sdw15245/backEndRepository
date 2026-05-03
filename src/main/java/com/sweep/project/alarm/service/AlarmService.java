package com.sweep.project.alarm.service;

import com.sweep.project.alarm.domain.Alarm;
import com.sweep.project.alarm.dto.AlarmCreateRequest;
import com.sweep.project.alarm.dto.AlarmDetailResponse;
import com.sweep.project.alarm.dto.AlarmSummaryResponse;
import com.sweep.project.alarm.dto.AlarmUpdateRequest;
import com.sweep.project.alarm.repository.AlarmRepository;
import com.sweep.project.fcm.domain.FcmToken;
import com.sweep.project.fcm.repository.FcmTokenRepository;
import com.sweep.project.member.service.SecurityMemberReadService;
import com.sweep.project.route.domain.Route;
import com.sweep.project.route.domain.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final AlarmRedisService alarmRedisService;
    private final FcmTokenRepository fcmTokenRepository;
    private final RouteRepository routeRepository;
    private final SecurityMemberReadService securityMemberReadService;

    public AlarmDetailResponse createAlarm(AlarmCreateRequest req) {

        Route route = routeRepository.findById(req.routeId())
                .orElseThrow(() -> new RuntimeException("없는 route입니다"));

        Alarm alarm = Alarm.builder()
                .member(securityMemberReadService.securityMemberRead())
                .route(route)
                .title(req.title())
                .checklist(req.checklist())
                .arrivalTime(req.arrivalTime())
                .startTime(req.startTime())
                .prepareTime(req.prepareTime())
                .interval(req.interval())
                .isLoop(Boolean.TRUE.equals(req.isLoop()))
                .day(req.day())
                .build();
        alarmRepository.save(alarm);

        Integer totalTime = route.getTotalTime();
        if (totalTime != null) {
            List<String> tokens = fcmTokenRepository.findAllByMemberId(alarm.getMemberId())
                    .stream().map(FcmToken::getToken).collect(Collectors.toList());
            alarmRedisService.registerTodayIfFirable(
                    alarm.getAlarmId(), alarm.getMemberId(), req.startTime(), req.arrivalTime(),
                    totalTime, req.prepareTime(), req.interval(), tokens);
        }

        return new AlarmDetailResponse(alarm);
    }

    @Transactional(readOnly = true)
    public List<AlarmSummaryResponse> getMyAlarms(Long memberId) {
        return alarmRepository.findAllByMember_IdAndDeletedFalse(memberId)
                .stream()
                .map(AlarmSummaryResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlarmDetailResponse getAlarmDetail(Long alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        return new AlarmDetailResponse(alarm);
    }

    public void updateAlarm(Long alarmId, AlarmUpdateRequest req) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        Route route = routeRepository.findById(req.routeId())
                .orElseThrow(() -> new RuntimeException("없는 route입니다"));
        alarmRedisService.deleteAlarmKeys(alarm.getMemberId(), alarm.getAlarmId());
        Integer newInterval = req.interval() != null ? req.interval() : alarm.getInterval();
        alarm.updateAlarm(route, req.arrivalTime(), req.startTime(), req.prepareTime(), newInterval,
                req.isLoop(), req.day(), req.title(), req.checklist());

        Integer totalTime = route.getTotalTime();
        if (totalTime != null) {
            List<String> tokens = fcmTokenRepository.findAllByMemberId(alarm.getMemberId())
                    .stream().map(FcmToken::getToken).collect(Collectors.toList());
            alarmRedisService.registerTodayIfFirable(
                    alarm.getAlarmId(), alarm.getMemberId(), req.startTime(), req.arrivalTime(),
                    totalTime, req.prepareTime(), newInterval, tokens);
        }
    }

    public void fireAndForgetUpdate(Long alarmId){
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        alarm.updateNeedCheck();
    }

    public void deleteAlarm(Long alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        alarm.updateDeleted();
        alarmRedisService.deleteAlarmKeys(alarm.getMemberId(), alarm.getAlarmId());
    }
}
