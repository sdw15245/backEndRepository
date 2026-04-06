package com.sweep.project.route;

import lombok.Getter;

@Getter
public class ApiResponse {

    private TrafficResponse trafficResponse;
    private BoardingInfo boardingInfo;

    public ApiResponse(TrafficResponse trafficResponse, BoardingInfo boardingInfo) {
        this.trafficResponse = trafficResponse;
        this.boardingInfo = boardingInfo;
    }
}
