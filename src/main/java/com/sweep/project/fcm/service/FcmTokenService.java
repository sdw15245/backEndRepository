package com.sweep.project.fcm.service;

import com.sweep.project.fcm.domain.FcmToken;
import com.sweep.project.fcm.repository.FcmTokenRepository;
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
}
