package com.sweep.project.route.preview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.redis.RoutePreviewRedisService;
import com.sweep.project.redis.RouteRedisService;
import com.sweep.project.route.TrafficResponse;
import com.sweep.project.route.bus.BusRoute;
import com.sweep.project.route.domain.Route;
import com.sweep.project.route.domain.RouteRepository;
import com.sweep.project.route.mixed.MixedRoute;
import com.sweep.project.route.preview.dto.RoutePreviewDto;
import com.sweep.project.route.preview.util.RouteColorResolver;
import com.sweep.project.route.subway.SubwayRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class RoutePreviewService {

    private static final String LOAD_LANE_URL = "https://api.odsay.com/v1/api/loadLane";

    @Value("${api-key.odsay}")
    private String odsayApiKey;

    private final RouteColorResolver routeColorResolver;
    private final RoutePreviewRedisService routePreviewRedisService;
    private final RouteRedisService routeRedisService;
    private final RouteRepository routeRepository;
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoutePreviewDto getRoutePreview(String mapObj) {
        return routePreviewRedisService.find(mapObj)
                .orElseGet(() -> {
                    RoutePreviewDto dto = loadRoutePreview(mapObj, UUID.randomUUID().toString());
                    routePreviewRedisService.save(mapObj, dto);
                    return dto;
                });
    }

    public RoutePreviewDto getRoutePreviewBySavedRouteId(Long routeId) {
        return routePreviewRedisService.findByRouteId(routeId)
                .orElseGet(() -> {
                    Route route = routeRepository.findById(routeId)
                            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "route를 찾을 수 없습니다."));

                    Optional<TrafficResponse> cachedRoute = routeRedisService.findOrCacheRoute(
                            route.getType(),
                            route.getStartY(),
                            route.getStartX(),
                            route.getEndY(),
                            route.getEndX(),
                            routeId,
                            route.getRouteData()
                    );

                    String mapObj = cachedRoute
                            .map(this::extractMapObj)
                            .filter(value -> !value.isBlank())
                            .orElseGet(() -> extractMapObj(route.getRouteData()));

                    if (mapObj == null || mapObj.isBlank()) {
                        throw new ResponseStatusException(NOT_FOUND, "routeData에 mapObj가 없습니다.");
                    }

                    RoutePreviewDto dto = loadRoutePreview(mapObj, String.valueOf(routeId));
                    routePreviewRedisService.saveByRouteId(routeId, dto);
                    return dto;
                });
    }

    private RoutePreviewDto loadRoutePreview(String mapObj, String responseRouteId) {
        String prefixedMapObj = mapObj.startsWith("0:0@") ? mapObj : "0:0@" + mapObj;

        String url = UriComponentsBuilder.fromHttpUrl(LOAD_LANE_URL)
                .queryParam("apiKey", odsayApiKey)
                .queryParam("mapObject", prefixedMapObj)
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return toRoutePreview(response.getBody(), responseRouteId);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ResponseStatusException(NOT_FOUND, "loadLane 데이터를 찾을 수 없습니다.", e);
            }
            throw new ResponseStatusException(BAD_GATEWAY, "ODsay loadLane 호출에 실패했습니다.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "노선 미리보기 처리 중 오류가 발생했습니다.", e);
        }
    }

    private RoutePreviewDto toRoutePreview(String json, String responseRouteId) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode lanesNode = root.path("result").path("lane");

        if (!lanesNode.isArray() || lanesNode.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "ODsay loadLane 응답에 lane 정보가 없습니다.");
        }

        List<RoutePreviewDto.SegmentDto> segments = new ArrayList<>();
        List<RoutePreviewDto.PointDto> allPoints = new ArrayList<>();

        for (JsonNode laneNode : lanesNode) {
            int trafficType = extractTrafficType(laneNode);
            int laneType = laneNode.path("type").asInt(0);
            List<RoutePreviewDto.PointDto> points = extractPoints(laneNode);

            if (points.isEmpty()) {
                continue;
            }

            allPoints.addAll(points);
            segments.add(RoutePreviewDto.SegmentDto.builder()
                    .trafficType(trafficType)
                    .trafficTypeLabel(toTrafficTypeLabel(trafficType))
                    .laneName("")
                    .color(routeColorResolver.resolveColorByLaneType(trafficType, laneType))
                    .strokeStyle(routeColorResolver.resolveStrokeStyle(trafficType))
                    .points(points)
                    .build());
        }

        if (segments.isEmpty() || allPoints.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "유효한 폴리라인 좌표가 없습니다.");
        }

        return RoutePreviewDto.builder()
                .routeId(responseRouteId)
                .bounds(calculateBounds(allPoints))
                .segments(segments)
                .build();
    }

    private int extractTrafficType(JsonNode laneNode) {
        if (laneNode.has("trafficType")) {
            return laneNode.path("trafficType").asInt(0);
        }
        return switch (laneNode.path("class").asInt(0)) {
            case 1 -> 2;
            case 2 -> 1;
            default -> 0;
        };
    }

    private String extractMapObj(TrafficResponse route) {
        if (route instanceof SubwayRoute subwayRoute) {
            return subwayRoute.getMapObj();
        }
        if (route instanceof BusRoute busRoute) {
            return busRoute.getMapObj();
        }
        if (route instanceof MixedRoute mixedRoute) {
            return mixedRoute.getMapObj();
        }
        return "";
    }

    private String extractMapObj(String routeData) {
        if (routeData == null || routeData.isBlank()) {
            return "";
        }
        try {
            return objectMapper.readTree(routeData).path("mapObj").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private List<RoutePreviewDto.PointDto> extractPoints(JsonNode laneNode) {
        List<RoutePreviewDto.PointDto> points = new ArrayList<>();

        parseGraphPosNode(laneNode.path("graphPos"), points);

        JsonNode sectionNode = laneNode.path("section");
        if (sectionNode.isArray()) {
            for (JsonNode sec : sectionNode) {
                parseGraphPosNode(sec.path("graphPos"), points);
                parsePointArray(sec.path("graphPos"), points);
            }
        }

        parsePointArray(laneNode.path("graphPos"), points);
        return points;
    }

    private void parseGraphPosNode(JsonNode graphPosNode, List<RoutePreviewDto.PointDto> points) {
        if (!graphPosNode.isTextual()) {
            return;
        }

        String graphPos = graphPosNode.asText("").trim();
        if (graphPos.isBlank()) {
            return;
        }

        String[] pairs = graphPos.split("\\s+");
        for (String pair : pairs) {
            String[] xy = pair.split(",");
            if (xy.length != 2) {
                continue;
            }
            try {
                points.add(RoutePreviewDto.PointDto.builder()
                        .x(Double.parseDouble(xy[0]))
                        .y(Double.parseDouble(xy[1]))
                        .build());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void parsePointArray(JsonNode node, List<RoutePreviewDto.PointDto> points) {
        if (!node.isArray()) {
            return;
        }

        for (JsonNode p : node) {
            if (!p.has("x") || !p.has("y")) {
                continue;
            }
            points.add(RoutePreviewDto.PointDto.builder()
                    .x(p.path("x").asDouble())
                    .y(p.path("y").asDouble())
                    .build());
        }
    }

    private String toTrafficTypeLabel(int trafficType) {
        return switch (trafficType) {
            case 1 -> "SUBWAY";
            case 2 -> "BUS";
            case 3 -> "WALK";
            default -> "UNKNOWN";
        };
    }

    private RoutePreviewDto.BoundsDto calculateBounds(List<RoutePreviewDto.PointDto> points) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (RoutePreviewDto.PointDto p : points) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }

        return RoutePreviewDto.BoundsDto.builder()
                .sw(RoutePreviewDto.PointDto.builder().x(minX).y(minY).build())
                .ne(RoutePreviewDto.PointDto.builder().x(maxX).y(maxY).build())
                .build();
    }
}
