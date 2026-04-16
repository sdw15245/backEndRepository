package com.sweep.project.fcm.controller;

import com.sweep.project.fcm.service.FcmTokenService;
import com.sweep.project.member.domain.Member;
import com.sweep.project.member.service.SecurityMemberReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fcm")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;
    private final SecurityMemberReadService securityMemberReadService;

    // 프론트가 로그인 후 토큰 저장 요청
    @PostMapping("/token")
    public void saveToken(@RequestBody String token) {
        Member member = securityMemberReadService.securityMemberRead();
        fcmTokenService.saveToken(member.getId(), token);
    }

    // 로그아웃 시 토큰 삭제
    @DeleteMapping("/token")
    public void deleteToken(@RequestBody String token) {
        fcmTokenService.deleteToken(token);
    }
}
