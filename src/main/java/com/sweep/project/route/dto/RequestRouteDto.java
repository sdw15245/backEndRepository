package com.sweep.project.route.dto;

import com.sweep.project.route.domain.PathSearchType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RequestRouteDto {

    @NotNull
    private PathSearchType type;
    @NotNull
    private double startX;
    @NotNull
    private double startY;
    @NotNull
    private double endX;
    @NotNull
    private double endY;
    @NotNull
    private LocalDateTime arrivalTime;

    public RequestRouteDto(PathSearchType type, double startX, double startY, double endX, double endY, LocalDateTime arrivalTime) {
        this.type = type;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.arrivalTime = arrivalTime;
    }
}
