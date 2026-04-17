package com.sweep.project.alarm.dto;

import java.time.LocalDateTime;

public record AlarmCreateRequest(
        Long routeTicketId,
        Long routeId,
        LocalDateTime arrivalTime,
        LocalDateTime startTime,
        Integer prepareTime,
        Integer interval,
        Boolean isLoop,
        String day
) {}
