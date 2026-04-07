package com.sweep.project.route.controller;

import com.sweep.project.route.*;
import com.sweep.project.route.bus.BusRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final TrafficRouteStragy trafficRouteStragy;

    @Value("${api-key.korea-data-portal}")
    private String seoulBusApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BUS_ARRIVAL_URL = "https://ws.bus.go.kr/api/rest/arrive/getArrInfoByRoute";


    /**
     * 서울버스 도착 정보 프록시.
     * GET /route/bus/arrival?stId=&busRouteId=&ord=
     */
    @GetMapping("/bus/arrival")
    public String getBusArrival(
            @RequestParam String stId,
            @RequestParam String busRouteId,
            @RequestParam(defaultValue = "0") int ord) {
        log.info("stdId:{}-busRouteId:{}",stId,busRouteId);
        String url = UriComponentsBuilder.fromHttpUrl(BUS_ARRIVAL_URL)
                .queryParam("serviceKey", seoulBusApiKey)
                .queryParam("stId", stId)
                .queryParam("busRouteId", busRouteId)
                .queryParam("ord", ord)
                .queryParam("resultType", "json")
                .toUriString();
        log.info("bus arrival proxy url: {}", url);
        return restTemplate.getForObject(url, String.class);
    }
    /**
     * 탑승 정보 조회.
     * GET /route/boarding?type=PATH_TYPE_SUBWAY&arrivalTime=2024-06-01T09:00:00
     *
     * routeIndex: getRoutes 결과 중 사용할 경로 번호 (기본값 0 = 첫 번째 경로)
     */
    @GetMapping("/boarding")
    public ApiResponse getBoardingInfo(
            @RequestParam PathSearchType type
            ,@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime) {
        List<? extends TrafficResponse> routes = trafficRouteStragy.getRoutes(type);
        if(routes.isEmpty()){
            return new ApiResponse(null,null);
        }
        List<BoardingInfo> boardingInfos=trafficRouteStragy.getBoardingInfo(type, arrivalTime,routes);
        return new ApiResponse(routes,boardingInfos);
    }
}
