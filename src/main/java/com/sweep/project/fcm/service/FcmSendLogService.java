package com.sweep.project.fcm.service;

import com.sweep.project.fcm.domain.FcmSendLog;
import com.sweep.project.fcm.dto.FcmSendLogResponse;
import com.sweep.project.fcm.repository.FcmSendLogBatchRepository;
import com.sweep.project.fcm.repository.FcmSendLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FcmSendLogService {

    private final FcmSendLogRepository fcmSendLogRepository;
    private final FcmSendLogBatchRepository fcmSendLogBatchRepository;

    // jdbcTemplate batchInsert 방식으로 교체
    public void saveSuccessLogs(List<FcmSendLog> logs) {
        if (logs.isEmpty()) {
            return;
        }

        fcmSendLogBatchRepository.batchInsert(logs);
    }

    @Transactional(readOnly = true)
    public List<FcmSendLogResponse> getDailySuccessLogs(Long memberId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        return fcmSendLogRepository
                .findAllByMemberIdAndSentAtGreaterThanEqualAndSentAtLessThanOrderBySentAtDesc(
                        memberId, start, end
                )
                .stream()
                .map(FcmSendLogResponse::new)
                .toList();
    }
}
