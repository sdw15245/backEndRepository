package com.sweep.project.alarm.controller;

import com.sweep.project.alarm.domain.Alarm;
import com.sweep.project.alarm.dto.AlarmDetailResponse;
import com.sweep.project.alarm.dto.AlarmSummaryResponse;
import com.sweep.project.alarm.service.AlarmService;
import com.sweep.project.member.service.SecurityMemberReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

    @Operation(
            summary = "알람 생성",
            description = """
                    새로운 알람을 생성합니다.
                    - isLoop=true 이면 prepareTime, interval, day 는 필수입니다.
                    - startTime 이 오늘 날짜이면 남은 트리거에 한해 즉시 Redis 에 등록됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알람 생성 성공",
                    content = @Content(schema = @Schema(implementation = Alarm.class))),
            @ApiResponse(responseCode = "400", description = "isLoop=true 인데 prepareTime·interval·day 누락",
                    content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization", description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true, example = "Bearer [tokenvalue]", in = ParameterIn.HEADER)
    @PostMapping
    public Alarm createAlarm(
            @Parameter(description = "알람에 연결할 경로 티켓 ID", required = true, example = "1")
            @RequestParam Long routeTicketId,

            @Parameter(description = "목적지 도착 예정 시각 (ISO 8601)", required = true, example = "2024-06-01T09:00:00")
            @RequestParam LocalDateTime arrivalTime,

            @Parameter(description = "알람 최초 발생 기준 시각. 오늘 날짜이면 남은 트리거를 즉시 Redis 에 등록", required = true, example = "2024-06-01T07:00:00")
            @RequestParam LocalDateTime startTime,

            @Parameter(description = "반복 알람 여부. true=매일/요일 반복, false=일회성", required = true, example = "true")
            @RequestParam Boolean isLoop,

            @Parameter(description = "출발 전 준비 시간 (분). isLoop=true 시 필수", example = "60")
            @RequestParam(required = false) Integer prepareTime,

            @Parameter(description = "준비 알람 발송 간격 (분). isLoop=true 시 필수", example = "20")
            @RequestParam(required = false) Integer interval,

            @Parameter(description = "반복 요일 (예: '월화수', 빈 문자열이면 매일). isLoop=true 시 필수", example = "월화수목금")
            @RequestParam(required = false) String day
    ) {
        if (Boolean.TRUE.equals(isLoop)) {
            if (prepareTime == null || interval == null || day == null) {
                throw new IllegalArgumentException("isLoop=true 일 때 prepareTime, interval, day 는 필수입니다.");
            }
        }
        return alarmService.createAlarm(routeTicketId,
                arrivalTime, startTime, prepareTime, interval, isLoop, day);
    }

    @Operation(
            summary = "내 알람 목록 조회",
            description = "로그인한 사용자의 알람 목록을 요약 정보(알람 ID, 반복 여부, 반복 요일, 출발 시각, 도착 시각)로 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlarmSummaryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization", description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true, example = "Bearer [tokenvalue]", in = ParameterIn.HEADER)
    @GetMapping
    public List<AlarmSummaryResponse> getMyAlarms() {
        Long memberId = securityMemberReadService.securityMemberRead().getId();
        return alarmService.getMyAlarms(memberId);
    }

    @Operation(
            summary = "알람 상세 조회",
            description = """
                    특정 알람의 상세 정보를 조회합니다.
                    - 알람 기본 정보(반복 여부, 요일, 출발/도착 시각, 준비 시간, 간격)
                    - 연결된 RouteTicket 정보(티켓 ID, 실시간 확인 필요 여부, 생성 시각)
                    - 연결된 Route 정보(경로 ID, 탐색 유형, 출발·도착 좌표, 총 소요 시간, 경로 원문 JSON)
                    를 함께 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AlarmDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 알람 ID",
                    content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization", description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true, example = "Bearer [tokenvalue]", in = ParameterIn.HEADER)
    @GetMapping("/{alarmId}")
    public AlarmDetailResponse getAlarmDetail(
            @Parameter(description = "조회할 알람 ID", required = true, example = "1")
            @PathVariable Long alarmId
    ) {
        return alarmService.getAlarmDetail(alarmId);
    }

    @Operation(
            summary = "알람 수정",
            description = """
                    특정 알람의 정보를 수정합니다.
                    - isLoop=true 이면 prepareTime, day 는 필수입니다.
                    - 수정 즉시 기존 Redis 키가 삭제되고, startTime 이 오늘이면 새 값으로 재등록됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알람 수정 성공"),
            @ApiResponse(responseCode = "400", description = "isLoop=true 인데 prepareTime·day 누락",
                    content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization", description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true, example = "Bearer [tokenvalue]", in = ParameterIn.HEADER)
    @PutMapping("/{alarmId}")
    public void updateAlarm(
            @Parameter(description = "수정할 알람 ID", required = true, example = "1")
            @PathVariable Long alarmId,

            @Parameter(description = "목적지 도착 예정 시각 (ISO 8601)", required = true, example = "2024-06-01T09:00:00")
            @RequestParam LocalDateTime arrivalTime,

            @Parameter(description = "알람 최초 발생 기준 시각. 오늘 날짜이면 새 값으로 Redis 재등록", required = true, example = "2024-06-01T07:00:00")
            @RequestParam LocalDateTime startTime,

            @Parameter(description = "반복 알람 여부. true=매일/요일 반복, false=일회성", required = true, example = "true")
            @RequestParam Boolean isLoop,

            @Parameter(description = "출발 전 준비 시간 (분). isLoop=true 시 필수", example = "60")
            @RequestParam(required = false) Integer prepareTime,

            @Parameter(description = "반복 요일 (예: '월화수', 빈 문자열이면 매일). isLoop=true 시 필수", example = "월화수목금")
            @RequestParam(required = false) String day
    ) {
        if (Boolean.TRUE.equals(isLoop)) {
            if (prepareTime == null || day == null) {
                throw new IllegalArgumentException("isLoop=true 일 때 prepareTime, day 는 필수입니다.");
            }
        }
        alarmService.updateAlarm(alarmId, arrivalTime, startTime, prepareTime, isLoop, day);
    }

    @Operation(summary = "알람 삭제", description = "특정 알람을 삭제(soft delete)합니다. 연관된 Redis 키도 함께 제거됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알람 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization", description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true, example = "Bearer [tokenvalue]", in = ParameterIn.HEADER)
    @DeleteMapping("/{alarmId}")
    public void deleteAlarm(
            @Parameter(description = "삭제할 알람 ID", required = true, example = "1")
            @PathVariable Long alarmId
    ) {
        alarmService.deleteAlarm(alarmId);
    }
}
