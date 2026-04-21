package com.sweep.project.docs.fcm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * NullRouteTicketWriter가 경로 소멸(routeData=null) 감지 시
 * FCM으로 전송하는 Silent Push 메시지 구조 참고용 스키마.
 *
 * Notification 블록 없음 → 시스템 트레이에 표시되지 않음.
 * 클라이언트가 백그라운드에서 routeTicketId를 읽어 경로 재선택 화면으로 유도.
 */
@Getter
@Schema(description = "경로 소멸(FIX) 알람 — FCM Data-only Silent Push 메시지")
public class FcmSilentPushSchema {

    @Schema(description = "수신 대상 FCM 디바이스 토큰", example = "fcm-device-token-abc")
    private String token;

    @Schema(description = "FCM data 페이로드 — Notification 없이 데이터만 전달 (silent push)")
    private DataBlock data;

    @Schema(description = "iOS APNs 설정 — content-available=1로 백그라운드 처리 허용")
    private ApnsBlock apns;

    @Getter
    @Schema(description = "FCM data 블록 (key-value 문자열 맵)")
    public static class DataBlock {

        @Schema(
            description = "재확인이 필요한 알람 ID (문자열). 클라이언트는 이 값으로 경로 재선택 화면을 표시",
            example = "5"
        )
        private String alarmId;
    }

    @Getter
    @Schema(description = "APNs 설정 블록 (iOS 전용)")
    public static class ApnsBlock {

        @Schema(description = "APNs aps 딕셔너리")
        private ApsBlock aps;

        @Getter
        @Schema(description = "APNs aps 딕셔너리")
        public static class ApsBlock {

            @Schema(
                description = """
                    content-available = 1.
                    앱이 백그라운드 상태일 때도 FCM 메시지를 수신하여 처리할 수 있도록 허용.
                    Notification 블록이 없으므로 사용자에게 배너·소리가 노출되지 않음.
                    """,
                example = "true"
            )
            private boolean contentAvailable;
        }
    }
}
