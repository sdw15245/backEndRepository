package com.sweep.project.fcm.controller;

import com.sweep.project.fcm.dto.FcmRequestDto;
import com.sweep.project.fcm.dto.FcmSendLogResponse;
import com.sweep.project.fcm.service.FcmSendLogService;
import com.sweep.project.fcm.service.FcmTokenService;
import com.sweep.project.member.domain.Member;
import com.sweep.project.member.service.SecurityMemberReadService;
import com.sweep.project.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "FCM 토큰", description = "FCM 푸시 토큰 관련 API")
@RestController
@RequestMapping("/fcm")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;
    private final SecurityMemberReadService securityMemberReadService;
    private final FcmSendLogService fcmSendLogService;

    @Operation(summary = "FCM 토큰 저장", description = "로그인 후 클라이언트의 FCM 토큰을 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 저장 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @PostMapping("/token")
    public void saveToken(@RequestBody FcmRequestDto fcmRequestDto) {
        Member member = securityMemberReadService.securityMemberRead();
        fcmTokenService.saveToken(member.getId(),fcmRequestDto.getToken());
    }

    @Operation(summary = "FCM 토큰 삭제", description = "로그아웃 시 클라이언트의 FCM 토큰을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @DeleteMapping("/token")
    public void deleteToken(@RequestBody FcmRequestDto fcmRequestDto) {
        fcmTokenService.deleteToken(fcmRequestDto.getToken());
    }

    @Operation(summary = "FCM 테스트 발송", description = "FCM 토큰 발송 테스트용 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "테스트 발송 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @GetMapping("/token")
    public void testTokenSending() {
        fcmTokenService.testSending();
    }

    @Operation(summary = "FCM 성공 발송 이력 조회", description = "로그인한 회원의 특정 날짜 FCM 성공 발송 이력을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "FCM 발송 이력 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않습니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "JWT 액세스 토큰 (Bearer 형식)",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @GetMapping("/messages")    // 조회 API, 로그인 회원 가져옴 -> 회원 ID 조회
    public ApiResponseUtil<List<FcmSendLogResponse>> getDailySuccessMessages(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Member member = securityMemberReadService.securityMemberRead();
        List<FcmSendLogResponse> logs = fcmSendLogService.getDailySuccessLogs(member.getId(), date);
        return ApiResponseUtil.SuccessApiResponse("FCM 발송 이력 조회 성공", logs);
    }
}
