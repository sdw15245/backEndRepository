package com.sweep.project.route.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NeedCheckAlertDto {
    private String fcmToken;
    private Long routeTicketId;

    public NeedCheckAlertDto(String fcmToken,Long routeTicketId) {
        this.fcmToken = fcmToken;
        this.routeTicketId = routeTicketId;
    }
}
