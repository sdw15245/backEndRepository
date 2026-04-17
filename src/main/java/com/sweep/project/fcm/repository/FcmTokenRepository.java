package com.sweep.project.fcm.repository;

import com.sweep.project.fcm.domain.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findAllByMemberId(Long memberId);

    Optional<FcmToken> findByToken(String token);

    @Transactional // 명시용 습관
    void deleteByToken(String token);

    @Modifying
    @Transactional // 원자성 보장
    void deleteByUpdatedAtBefore(LocalDateTime threshold);
}
