package com.sweep.project.fcm.repository;

import com.sweep.project.fcm.domain.FcmSendLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

//하루 단위 조회
public interface FcmSendLogRepository extends JpaRepository<FcmSendLog, Long> {

    List<FcmSendLog> findAllByMemberIdAndSentAtGreaterThanEqualAndSentAtLessThanOrderBySentAtDesc(
            Long memberId,
            LocalDateTime start,
            LocalDateTime end
    );
}
