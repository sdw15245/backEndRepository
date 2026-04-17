package com.sweep.project.alarm.dto;

import java.time.LocalDateTime;

public record AlarmUpdateRequest(
        LocalDateTime arrivalTime,
        LocalDateTime startTime,
        Integer prepareTime,
        Integer interval,
        Boolean isLoop,
        String day
) {}
