package com.sweep.project.fcm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class FcmSendLog {           // FCM 메시지 전송 성공 이력을 저장하는 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    private Long alarmId;

    private String alarmType;

    @Column(nullable = false, columnDefinition = "text")
    private String token;

    private String title;

    @Column(columnDefinition = "text")
    private String body;

    private String firebaseMessageId;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Builder
    public FcmSendLog(Long memberId, Long alarmId, String alarmType, String token,
                      String title, String body, String firebaseMessageId,
                      LocalDateTime sentAt) {
        this.memberId = memberId;       // 로그인 회원 ID
        this.alarmId = alarmId;         // 어떤 알람인지
        this.alarmType = alarmType;     // 어떤 종류 알람
        this.token = token;             // 어떤 기기로 보냈는지
        this.title = title;             // 알림 상단에 보이는 제목
        this.body = body;               // 알림 내용
        this.firebaseMessageId = firebaseMessageId;     // Firebase가 성공적으로 보냈다고 반환해준 메시지 ID
        this.sentAt = sentAt;   // 언제 보냈는지 (FCM 성공 로그 저장된시간)
    }
}
