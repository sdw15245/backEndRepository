package com.sweep.project.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
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
}
