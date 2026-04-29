package com.sweep.project.fcm.service;

import com.sweep.project.fcm.domain.FcmSendLog;
import com.sweep.project.fcm.dto.FcmSendLogResponse;
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

    // 성공 로그 DB 저장
    public void saveSuccessLog(Long memberId, Long alarmId, String alarmType, String token,
                               String title, String body, String firebaseMessageId) {
        fcmSendLogRepository.save(FcmSendLog.builder()
                .memberId(memberId)
                .alarmId(alarmId)
                .alarmType(alarmType)
                .token(token)
                .title(title)
                .body(body)
                .firebaseMessageId(firebaseMessageId)
                .sentAt(LocalDateTime.now())
                .build());
    }

    public void saveSuccessLogs(List<FcmSendLog> logs) {
        fcmSendLogRepository.saveAll(logs);
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
