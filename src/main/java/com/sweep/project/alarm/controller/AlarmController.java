package com.sweep.project.alarm.controller;

import com.sweep.project.alarm.domain.Alarm;
import com.sweep.project.alarm.service.AlarmService;
import com.sweep.project.member.service.SecurityMemberReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;
    private final SecurityMemberReadService securityMemberReadService;

    @PostMapping
    public Alarm createAlarm(@RequestParam Long routeTicketId,
                             @RequestParam Long routeId,
                             @RequestParam LocalDateTime arrivalTime,
                             @RequestParam LocalDateTime startTime,
                             @RequestParam Integer prepareTime,
                             @RequestParam Integer interval,
                             @RequestParam Boolean isLoop,
                             @RequestParam String day) {
        Long memberId = securityMemberReadService.securityMemberRead().getId();
        return alarmService.createAlarm(memberId, routeTicketId, routeId,
                arrivalTime, startTime, prepareTime, interval, isLoop, day);
    }

    @GetMapping
    public List<Alarm> getMyAlarms() {
        Long memberId = securityMemberReadService.securityMemberRead().getId();
        return alarmService.getMyAlarms(memberId);
    }

    @PutMapping("/{alarmId}")
    public void updateAlarm(@PathVariable Long alarmId,
                            @RequestParam LocalDateTime arrivalTime,
                            @RequestParam LocalDateTime startTime,
                            @RequestParam Integer prepareTime,
                            @RequestParam Boolean isLoop,
                            @RequestParam String day) {
        alarmService.updateAlarm(alarmId, arrivalTime, startTime, prepareTime, isLoop, day);
    }

    @DeleteMapping("/{alarmId}")
    public void deleteAlarm(@PathVariable Long alarmId) {
        alarmService.deleteAlarm(alarmId);
    }
}