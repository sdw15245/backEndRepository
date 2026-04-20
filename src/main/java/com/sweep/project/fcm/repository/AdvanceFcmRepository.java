package com.sweep.project.fcm.repository;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sweep.project.fcm.domain.QFcmToken;
import com.sweep.project.fcm.service.FcmSendService;
import com.sweep.project.member.domain.Member;
import com.sweep.project.member.domain.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.sweep.project.fcm.domain.QFcmToken.*;
import static com.sweep.project.member.domain.QMember.*;

@Repository
@RequiredArgsConstructor
public class AdvanceFcmRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final FcmSendService fcmSendService;

    public void testSending(Member member1){
        List<String> tokens=jpaQueryFactory.select(fcmToken.token)
                .from(fcmToken)
                .join(member)
                .on(member.id.eq(fcmToken.memberId))
                .fetch();
        List<Message> messages=tokens.stream().map(x->{
            return   Message.builder()
                    .setToken(x)
                    .setNotification(Notification.builder()
                            .setTitle("전송 테스트")
                            .setBody("전송 테스트")
                            .build())
                    .build();
        }).toList();
        fcmSendService.bulkPush(messages);
    }
}
