package com.sweep.project.fcm.scheduler;

import com.sweep.project.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmTokenScheduler {

    private final FcmTokenRepository fcmTokenRepository;


     // 매일 새벽 3시에 실행 (Cron = 초 분 시 일 월 요일)
     // 1개월(30일) 동안 갱신되지 않은 토큰을 삭제

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupStaleTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(1);

        log.info("FCM 비활성 토큰 청소 시작 (기준 시점: {})", threshold);
        fcmTokenRepository.deleteByUpdatedAtBefore(threshold);
        log.info("FCM 비활성 토큰 청소 완료");
    }
}