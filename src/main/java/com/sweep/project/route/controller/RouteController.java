package com.sweep.project.route.controller;

import com.sweep.project.route.bus.BusArrivalInfo;
import com.sweep.project.route.bus.BusArrivalService;
import com.sweep.project.route.bus.BusBoardingInfo;
import com.sweep.project.route.*;
import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.domain.Route;
import com.sweep.project.route.domain.RouteDbService;
import com.sweep.project.route.domain.RouteResponse;
import com.sweep.project.route.mixed.MixedBoardingInfo;
import com.sweep.project.route.mixed.SegmentBoardingInfo;
import com.sweep.project.redis.RouteRedisService;
import com.sweep.project.route.preview.service.RoutePreviewMetaRedisService;
import com.sweep.project.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
@Slf4j
public class RouteController {

    private final TrafficRouteStragy trafficRouteStragy;
    private final BusArrivalService busArrivalService;
    private final RouteDbService routeDbService;
    private final RouteRedisService routeRedisService;
    private final RoutePreviewMetaRedisService routePreviewMetaRedisService;

    /**
     * 버스 도착 정보 조회.
     * GET /route/bus/arrival?stId=&busRouteId=&ord=&providerCode=
     *
     * providerCode: 2 = 경기도, 4 = 서울 (기본값)
     * ord가 0이면 BIS API를 통해 자동 조회한다.
     */
    @Operation(summary = "버스의 위치를 조회",description = "특정 노선의 버스가 특정 정류장에 대한 도착정보를 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    headers = {
                            @Header(name = "Authorization", description = "Bearer [Access JWT 토큰]",
                                    schema = @Schema(type = "string")),
                    },
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "권한이 부족합니다.",
                    content = @Content(schema = @Schema()))
    })
    @GetMapping("/bus/arrival")
    public ApiResponseUtil<BusArrivalInfo> getBusArrival(
            @Parameter(description = "버스 정류소 ID (BIS 기준)", example = "100000080", required = true)
            @RequestParam String stId,
            @Parameter(description = "버스 노선 ID (BIS 기준)", example = "100100118", required = true)
            @RequestParam String busRouteId,
            @Parameter(description = "노선 내 정류소 순번. 0이면 BIS API로 자동 조회", example = "0")
            @RequestParam(defaultValue = "0") int ord,
            @Parameter(description = "버스 정보 제공 기관 코드. 2=경기도, 4=서울(기본값)", example = "4")
            @RequestParam(defaultValue = "4") int providerCode) {
        return ApiResponseUtil.SuccessApiResponse("ok",busArrivalService.getBusArrival(stId, busRouteId, ord, providerCode));
    }

    /**
     * 탑승 정보 조회.
     * GET /route/boarding?type=PATH_TYPE_SUBWAY&arrivalTime=2024-06-01T09:00:00
     */
    @Operation(summary = "도착지-목적지 루트 조회",description = "도착지 목적지 간의 교통수단에 따른 루트를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    headers = {
                            @Header(name = "Authorization", description = "Bearer [Access JWT 토큰]",
                                    schema = @Schema(type = "string")),
                    },
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "권한이 부족합니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "요청시 토큰값을 넣어주셔야됩니다.",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @GetMapping("/boarding")
    public ApiResponseUtil<RouteResponse> getBoardingInfo(
            @Parameter(description = "경로 탐색 유형. PATH_TYPE_ANYONE, PATH_TYPE_SUBWAY, PATH_TYPE_BUS", example = "PATH_TYPE_SUBWAY", required = true)
            @RequestParam PathSearchType type,
            @Parameter(description = "출발지 위도", example = "37.5665", required = true)
            @RequestParam double startLat,
            @Parameter(description = "출발지 경도", example = "126.9780", required = true)
            @RequestParam double startLon,
            @Parameter(description = "목적지 위도", example = "37.4979", required = true)
            @RequestParam double endLat,
            @Parameter(description = "목적지 경도", example = "127.0276", required = true)
            @RequestParam double endLon,
            @Parameter(description = "목적지 도착 희망 시각 (ISO 8601 형식)", example = "2024-06-01T09:00:00", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime) {
        List<? extends TrafficResponse> routes = trafficRouteStragy.getRoutes(type, startLat, startLon, endLat, endLon);
        if (routes.isEmpty()) {
            return ApiResponseUtil.SuccessApiResponse("ok", new RouteResponse(null, null));
        }
        routePreviewMetaRedisService.saveRouteMetas(routes);
        List<BoardingInfo> boardingInfos = trafficRouteStragy.getBoardingInfo(type, arrivalTime, routes);
        return ApiResponseUtil.SuccessApiResponse("ok",new RouteResponse(routes, boardingInfos));
    }


    @Operation(summary = "특정 루트 id값에 대해서 subway타입인 지하철 시간표를 조회해서 보여줌." +
            " latestBoardingTime  이컬럼을 쓰씨면될거같습니다." +
            "버스의 경우에는 구성되는 버스와,정류소 데이터를 기반으로 실제 도착 몇분전인지 최대 2개까지 보여줍니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    headers = {
                            @Header(name = "Authorization", description = "Bearer [Access JWT 토큰]",
                                    schema = @Schema(type = "string")),
                    },
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "권한이 부족합니다.",
                    content = @Content(schema = @Schema()))
    })
    @Parameter(name = "Authorization",
            description = "요청시 토큰값을 넣어주셔야됩니다.",
            required = true,
            example = "Bearer [tokenvalue]",
            in = ParameterIn.HEADER)
    @GetMapping("/detail/{id}")
    public ApiResponseUtil<List<BoardingInfo>> getSubwayBoardingInfo(
            @Parameter(description = "경로 id값", required = true)
            @PathVariable(name = "id") Long id,
            @Parameter(description = "경로 탐색 유형. PATH_TYPE_ANYONE, PATH_TYPE_SUBWAY, PATH_TYPE_BUS", example = "PATH_TYPE_SUBWAY", required = true)
            @RequestParam PathSearchType type,
            @Parameter(description = "출발지 위도", example = "37.5665", required = true)
            @RequestParam double startLat,
            @Parameter(description = "출발지 경도", example = "126.9780", required = true)
            @RequestParam double startLon,
            @Parameter(description = "목적지 위도", example = "37.4979", required = true)
            @RequestParam double endLat,
            @Parameter(description = "목적지 경도", example = "127.0276", required = true)
            @RequestParam double endLon,
            @Parameter(description = "목적지 도착 희망 시각 (ISO 8601 형식)", example = "2024-06-01T09:00:00", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime) {

        String timeHHmm = arrivalTime.format(DateTimeFormatter.ofPattern("HHmm"));
        int dayCode = switch (arrivalTime.getDayOfWeek()) {
            case SATURDAY -> 2;
            case SUNDAY   -> 3;
            default       -> 1;
        };

        // 1. 기존 boarding hash 에서 routeId 단건 조회
        Optional<BoardingInfo> cached =
                routeRedisService.findBoardingById(type, timeHHmm, dayCode, startLat, startLon, endLat, endLon, id);
        if (cached.isPresent()) {
            BoardingInfo boardingInfo = cached.get();
            enrichBusSegmentsWithRealtime(boardingInfo);
            return ApiResponseUtil.SuccessApiResponse("ok", List.of(boardingInfo));
        }

        // 2. 캐시 미스 → DB에서 TrafficResponse 조회
        List<TrafficResponse> trafficResponses = routeDbService.findById(id);
        if (trafficResponses.isEmpty()) {
            return ApiResponseUtil.SuccessApiResponse("ok", List.of());
        }

        // 3. AOP 없이 단건 boarding info 계산
        BoardingInfo boardingInfo =
                trafficRouteStragy.getBoardingInfoSingle(type, arrivalTime, trafficResponses.get(0));

        // 4. 기존 boarding hash 에 routeId 기준으로 저장 (실시간 버스 정보 제외)
        routeRedisService.saveBoardingById(
                type, timeHHmm, dayCode, startLat, startLon, endLat, endLon, id, boardingInfo);

        // 5. 버스 구간 실시간 도착 정보 보강
        enrichBusSegmentsWithRealtime(boardingInfo);

        return ApiResponseUtil.SuccessApiResponse("ok", List.of(boardingInfo));
    }

    /**
     * MixedBoardingInfo 내 버스 구간(trafficType=2)마다 getBusArrival을 호출해
     * 실시간 도착 정보를 arrivingBuses 에 채운다.
     * localStationId 또는 localBusId 가 없는 구간은 건너뛰고, API 실패 시 경고만 남긴다.
     */
    private void enrichBusSegmentsWithRealtime(BoardingInfo boardingInfo) {
        if (!(boardingInfo instanceof MixedBoardingInfo mixed)) return;
        for (SegmentBoardingInfo seg : mixed.getSegmentBoardingInfos()) {
            if (seg.getTrafficType() != TrafficType.TRAFFIC_TYPE_BUS.trafficNumber) continue;
            String stId = seg.getLocalStationId();
            String busRouteId = seg.getLocalBusId();
            if (stId == null || stId.isBlank() || busRouteId == null || busRouteId.isBlank()) continue;
            try {
                BusArrivalInfo arrivalInfo = busArrivalService.getBusArrival(
                        stId, busRouteId, seg.getStartStopOrder(), seg.getStationProviderCode());
                List<BusBoardingInfo.ArrivingBus> buses = new ArrayList<>();
                if (arrivalInfo.getArrmsg1() != null && !arrivalInfo.getArrmsg1().isBlank()) {
                    buses.add(new BusBoardingInfo.ArrivingBus(
                            null, arrivalInfo.getArrmsg1(), arrivalInfo.getTraTime1(),
                            0, null, 0, false, 0.0, 0.0));
                }
                if (arrivalInfo.getArrmsg2() != null && !arrivalInfo.getArrmsg2().isBlank()) {
                    buses.add(new BusBoardingInfo.ArrivingBus(
                            null, arrivalInfo.getArrmsg2(), arrivalInfo.getTraTime2(),
                            0, null, 0, false, 0.0, 0.0));
                }
                seg.setArrivingBuses(buses);
            } catch (Exception e) {
                log.warn("[detail] 버스 실시간 정보 조회 실패: stId={}, busRouteId={}", stId, busRouteId, e);
            }
        }
    }
}
