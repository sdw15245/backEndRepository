package com.sweep.project.alarm.controller;

import com.sweep.project.alarm.domain.Alarm;
import com.sweep.project.alarm.dto.AlarmCreateRequest;
import com.sweep.project.alarm.dto.AlarmUpdateRequest;
import com.sweep.project.alarm.service.AlarmService;
import com.sweep.project.member.service.SecurityMemberReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;
    private final SecurityMemberReadService securityMemberReadService;

    @PostMapping
    public Alarm createAlarm(@RequestBody AlarmCreateRequest request) {
        Long memberId = securityMemberReadService.securityMemberRead().getId();
        return alarmService.createAlarm(memberId, request);
    }

    @GetMapping
    public List<Alarm> getMyAlarms() {
        Long memberId = securityMemberReadService.securityMemberRead().getId();
        return alarmService.getMyAlarms(memberId);
    }

    @PutMapping("/{alarmId}")
    public void updateAlarm(@PathVariable Long alarmId,
                            @RequestBody AlarmUpdateRequest request) {
        alarmService.updateAlarm(alarmId, request);
    }

    @DeleteMapping("/{alarmId}")
    public void deleteAlarm(@PathVariable Long alarmId) {
        alarmService.deleteAlarm(alarmId);
    }
}
