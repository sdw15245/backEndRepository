package com.sweep.project.alarm.controller;

import com.sweep.project.alarm.domain.Alarm;
import com.sweep.project.alarm.service.AlarmService;
import com.sweep.project.member.service.SecurityMemberReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "알람", description = "알람 관련 API")
@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;
    private final SecurityMemberReadService securityMemberReadService;

    @Operation(summary = "알람 생성", description = "새로운 알람을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알람 생성 성공",
                    content = @Content(schema = @Schema(implementation = Alarm.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
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

    @Operation(summary = "내 알람 목록 조회", description = "로그인한 사용자의 알람 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Alarm.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @GetMapping
    public List<Alarm> getMyAlarms() {
        Long memberId = securityMemberReadService.securityMemberRead().getId();
        return alarmService.getMyAlarms(memberId);
    }

    @Operation(summary = "알람 수정", description = "특정 알람의 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알람 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @PutMapping("/{alarmId}")
    public void updateAlarm(@PathVariable Long alarmId,
                            @RequestParam LocalDateTime arrivalTime,
                            @RequestParam LocalDateTime startTime,
                            @RequestParam Integer prepareTime,
                            @RequestParam Boolean isLoop,
                            @RequestParam String day) {
        alarmService.updateAlarm(alarmId, arrivalTime, startTime, prepareTime, isLoop, day);
    }

    @Operation(summary = "알람 삭제", description = "특정 알람을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알람 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @DeleteMapping("/{alarmId}")
    public void deleteAlarm(@PathVariable Long alarmId) {
        alarmService.deleteAlarm(alarmId);
    }
}
