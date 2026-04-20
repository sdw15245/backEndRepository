package com.sweep.project.fcm.service;

import com.sweep.project.fcm.domain.FcmToken;
import com.sweep.project.fcm.repository.AdvanceFcmRepository;
import com.sweep.project.fcm.repository.FcmTokenRepository;
import com.sweep.project.member.domain.Member;
import com.sweep.project.member.service.SecurityMemberReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final SecurityMemberReadService securityMemberReadService;
    private final AdvanceFcmRepository advanceFcmRepository;

    // 토큰 저장 (이미 있으면 시간만 갱신)
    public void saveToken(Long memberId, String token) {
        fcmTokenRepository.findByToken(token)
                .ifPresentOrElse(
                        existingToken -> existingToken.updateTimestamp(),
                        () -> fcmTokenRepository.save(FcmToken.builder()
                                .memberId(memberId)
                                .token(token)
                                .build())
                );

//        if (fcmTokenRepository.findByToken(token).isPresent()) {
//            return;
//        }
//        fcmTokenRepository.save(FcmToken.builder()
//                .memberId(memberId)
//                .token(token)
//                .build());
    }

    // 토큰 삭제 (로그아웃 시)
    public void deleteToken(String token) {
        fcmTokenRepository.deleteByToken(token);
    }

    //fcm 토큰 전송 테스트용
    public void testSending(){
        Member member=securityMemberReadService.securityMemberRead();
        advanceFcmRepository.testSending(member);
    }
}
