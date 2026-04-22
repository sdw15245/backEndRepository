package com.sweep.project.alarm.service;

import com.sweep.project.alarm.domain.Alarm;
import com.sweep.project.alarm.dto.AlarmDetailResponse;
import com.sweep.project.alarm.dto.AlarmSummaryResponse;
import com.sweep.project.alarm.repository.AlarmRepository;
import com.sweep.project.fcm.domain.FcmToken;
import com.sweep.project.fcm.repository.FcmTokenRepository;
import com.sweep.project.route.domain.RouteTicket;
import com.sweep.project.route.domain.RouteTicketRepository;
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
    private final RouteTicketRepository routeTicketRepository;

    // 생성
    public Alarm createAlarm(Long routeTicketId,
                             LocalDateTime arrivalTime, LocalDateTime startTime,
                             Integer prepareTime, Integer interval,
                             Boolean isLoop, String day) {
        RouteTicket routeTicket = routeTicketRepository.findById(routeTicketId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 RouteTicket"));

        Alarm alarm = Alarm.builder()
                .routeTicket(routeTicket)
                .arrivalTime(arrivalTime)
                .startTime(startTime)
                .prepareTime(prepareTime)
                .interval(interval)
                .isLoop(isLoop)
                .day(day)
                .build();
        alarmRepository.save(alarm);

        // startTime이 오늘이면 즉시 Redis 등록
        Integer totalTime = routeTicket.getRoute().getTotalTime();
        if (totalTime != null) {
            List<String> tokens = fcmTokenRepository.findAllByMemberId(routeTicket.getMember().getId())
                    .stream().map(FcmToken::getToken).collect(Collectors.toList());
            alarmRedisService.registerTodayIfFirable(
                    alarm.getAlarmId(), alarm.getMemberId(), startTime, arrivalTime,
                    totalTime, prepareTime, interval, tokens);
        }

        return alarm;
    }

    // 목록 조회 (요약 정보)
    @Transactional(readOnly = true)
    public List<AlarmSummaryResponse> getMyAlarms(Long memberId) {
        return alarmRepository.findAllByRouteTicket_Member_IdAndDeletedFalse(memberId)
                .stream()
                .map(AlarmSummaryResponse::new)
                .collect(Collectors.toList());
    }

    // 상세 조회 (알람 + RouteTicket + Route 정보)
    @Transactional(readOnly = true)
    public AlarmDetailResponse getAlarmDetail(Long alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        return new AlarmDetailResponse(alarm);
    }

    // 수정 — 기존 Redis 키 삭제 후 당일 조건 충족 시 즉시 재등록
    public void updateAlarm(Long alarmId, LocalDateTime arrivalTime,
                            LocalDateTime startTime, Integer prepareTime,
                            Boolean isLoop, String day) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        alarmRedisService.deleteAlarmKeys(alarm.getMemberId(), alarm.getAlarmId());
        alarm.updateAlarm(arrivalTime, startTime, prepareTime, isLoop, day);

        Integer totalTime = alarm.getRouteTicket().getRoute().getTotalTime();
        if (totalTime != null) {
            List<String> tokens = fcmTokenRepository.findAllByMemberId(alarm.getMemberId())
                    .stream().map(FcmToken::getToken).collect(Collectors.toList());
            alarmRedisService.registerTodayIfFirable(
                    alarm.getAlarmId(), alarm.getMemberId(), startTime, arrivalTime,
                    totalTime, prepareTime, alarm.getInterval(), tokens);
        }
    }

    public void deleteAlarm(Long alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 알람"));
        alarm.updateDeleted();
        alarmRedisService.deleteAlarmKeys(alarm.getMemberId(), alarm.getAlarmId());
    }

    // RouteTicket 삭제 시 연관 알람 일괄 soft-delete + Redis 키 정리
    public void deleteAlarmsByRouteTicket(Long routeTicketId) {
        List<Alarm> alarms = alarmRepository.findAllByRouteTicket_IdAndDeletedFalse(routeTicketId);
        for (Alarm alarm : alarms) {
            alarm.updateDeleted();
            alarmRedisService.deleteAlarmKeys(alarm.getMemberId(), alarm.getAlarmId());
        }
        log.info("[AlarmService] RouteTicket 연관 알람 삭제 — routeTicketId={} 건수={}", routeTicketId, alarms.size());
    }
}
