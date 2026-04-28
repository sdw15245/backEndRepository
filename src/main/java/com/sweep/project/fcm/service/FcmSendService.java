package com.sweep.project.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.sweep.project.redis.RedisMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(FirebaseMessaging.class)
public class FcmSendService {

    // frontend에서 받아온 토큰을 제목과 내용담아서 firebase로 전송하기
    // 여러명한테 동시 알림과 전송 실패처리 아직없음
    private final FirebaseMessaging firebaseMessaging;
    private final FcmSendLogService fcmSendLogService;

    public String sendPush(String token, String title, String body) throws FirebaseMessagingException {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        return firebaseMessaging.send(message);
    }

    public void bulkPush(List<Message> data){
        try {
            firebaseMessaging.sendEach(data);
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException(e);
        }
    }

    // Firebase로 FCM 메시지 일괄 전송, 성공한 메시지만 발송이력 저장
    public void bulkPushWithLog(List<Message> data, List<RedisMessageDto> metadata, String title, String body) {
        if (data.size() != metadata.size()) {
            throw new IllegalArgumentException("FCM 메시지와 로그 메타데이터 수가 일치하지 않습니다.");
        }

        try {
            BatchResponse response = firebaseMessaging.sendEach(data);

            for (int i = 0; i < response.getResponses().size(); i++) {
                SendResponse sendResponse = response.getResponses().get(i);

                // 실패한 전송은 조회 이력에 남기지 않음
                if (!sendResponse.isSuccessful()) {
                    log.warn("FCM 전송 실패: {}", sendResponse.getException().getMessage());
                    continue;
                }

                RedisMessageDto logData = metadata.get(i);
                fcmSendLogService.saveSuccessLog(
                        Long.valueOf(logData.getMemberId()),
                        Long.valueOf(logData.getAlarmId()),
                        logData.getAlarmType(),
                        logData.getToken(),
                        title,
                        body,
                        sendResponse.getMessageId()
                );
            }
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException(e);
        }
    }
}

/*
          [로그 저장 값]
 memberId ->            누구에게 보냈는지
 alarmId ->             어떤 알람에서 발생했는지
 alarmType ->           출발 알림/준비 알림 구분
 token ->               발송 대상 기기 토큰
 title/body ->          실제 FCM 메시지 제목/내용
 firebaseMessageId ->   Firebase 성공 응답 메시지 ID
 */