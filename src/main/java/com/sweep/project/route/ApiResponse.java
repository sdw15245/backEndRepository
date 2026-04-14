package com.sweep.project.route;

import lombok.Getter;

import java.util.List;

@Getter
public class ApiResponse {

    private List<? extends TrafficResponse> trafficResponse;
    private List<BoardingInfo> boardingInfo;
    public ApiResponse(List<? extends TrafficResponse> trafficResponse, List<BoardingInfo> boardingInfo) {
        this.trafficResponse = trafficResponse;
        this.boardingInfo = boardingInfo;
    }
}
