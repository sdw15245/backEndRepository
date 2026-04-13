package com.sweep.project.fcm.repository;

import com.sweep.project.fcm.domain.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findAllByMemberId(Long memberId);

    Optional<FcmToken> findByToken(String token);

    void deleteByToken(String token);
}
