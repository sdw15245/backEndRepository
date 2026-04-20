package com.sweep.project.fcm.controller;

import com.sweep.project.fcm.service.FcmTokenService;
import com.sweep.project.member.domain.Member;
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

@Tag(name = "FCM 토큰", description = "FCM 푸시 토큰 관련 API")
@RestController
@RequestMapping("/fcm")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;
    private final SecurityMemberReadService securityMemberReadService;

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
    public void saveToken(@RequestBody String token) {
        Member member = securityMemberReadService.securityMemberRead();
        fcmTokenService.saveToken(member.getId(), token);
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
    public void deleteToken(@RequestBody String token) {
        fcmTokenService.deleteToken(token);
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
}
