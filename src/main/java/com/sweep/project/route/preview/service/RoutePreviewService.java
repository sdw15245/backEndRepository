package com.sweep.project.route.preview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.route.preview.dto.RoutePreviewDto;
import com.sweep.project.route.preview.util.RouteColorResolver;
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
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * ODsay loadLane 응답을 카카오맵 렌더링 DTO로 변환한다.
 */
@Service
@RequiredArgsConstructor
public class RoutePreviewService {

    private static final String LOAD_LANE_URL = "https://api.odsay.com/v1/api/loadLane";

    @Value("${api-key.odsay}")
    private String odsayApiKey;

    private final RouteColorResolver routeColorResolver;

    // 프로젝트 전역 RestTemplate Bean이 없을 수 있어 내부에서 기본 인스턴스를 사용한다.
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * mapObj를 받아 ODsay loadLane을 호출하고 RoutePreviewDto로 반환한다.
     */
    public RoutePreviewDto getRoutePreview(String mapObj) {
        String prefixedMapObj = mapObj.startsWith("0:0@") ? mapObj : "0:0@" + mapObj;

        String url = UriComponentsBuilder.fromHttpUrl(LOAD_LANE_URL)
                .queryParam("apiKey", odsayApiKey)
                .queryParam("mapObject", prefixedMapObj)
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return toRoutePreview(response.getBody());
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

    private RoutePreviewDto toRoutePreview(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode resultNode = root.path("result");
        JsonNode lanesNode = resultNode.path("lane");

        if (!lanesNode.isArray() || lanesNode.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "ODsay loadLane 응답에 lane 정보가 없습니다.");
        }

        List<RoutePreviewDto.SegmentDto> segments = new ArrayList<>();
        List<RoutePreviewDto.PointDto> allPoints = new ArrayList<>();

        for (JsonNode laneNode : lanesNode) {
            int trafficType = laneNode.path("trafficType").asInt(0);
            int busType = extractBusType(laneNode);
            String laneName = extractLaneName(laneNode);
            List<RoutePreviewDto.PointDto> points = extractPoints(laneNode);

            if (points.isEmpty()) {
                continue;
            }

            allPoints.addAll(points);

            segments.add(RoutePreviewDto.SegmentDto.builder()
                    .trafficType(trafficType)
                    .trafficTypeLabel(toTrafficTypeLabel(trafficType))
                    .laneName(laneName)
                    .color(routeColorResolver.resolveColor(trafficType, laneName, busType))
                    .strokeStyle(routeColorResolver.resolveStrokeStyle(trafficType))
                    .points(points)
                    .build());
        }

        if (segments.isEmpty() || allPoints.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "유효한 폴리라인 좌표가 없습니다.");
        }

        return RoutePreviewDto.builder()
                .routeId(UUID.randomUUID().toString())
                .bounds(calculateBounds(allPoints))
                .segments(segments)
                .build();
    }

    private int extractBusType(JsonNode laneNode) {
        if (laneNode.has("busType")) {
            return laneNode.path("busType").asInt(0);
        }
        return laneNode.path("type").asInt(0);
    }

    private String extractLaneName(JsonNode laneNode) {
        if (laneNode.hasNonNull("name") && !laneNode.path("name").asText().isBlank()) {
            return laneNode.path("name").asText();
        }
        if (laneNode.hasNonNull("busNo") && !laneNode.path("busNo").asText().isBlank()) {
            return laneNode.path("busNo").asText();
        }
        if (laneNode.hasNonNull("laneName") && !laneNode.path("laneName").asText().isBlank()) {
            return laneNode.path("laneName").asText();
        }
        return "";
    }

    /**
     * loadLane 응답의 section/graphPos를 폭넓게 파싱한다.
     */
    private List<RoutePreviewDto.PointDto> extractPoints(JsonNode laneNode) {
        List<RoutePreviewDto.PointDto> points = new ArrayList<>();

        // 1) lane.graphPos 직접 제공 케이스
        parseGraphPosNode(laneNode.path("graphPos"), points);

        // 2) lane.section 배열 내부 graphPos 케이스
        JsonNode sectionNode = laneNode.path("section");
        if (sectionNode.isArray()) {
            for (JsonNode sec : sectionNode) {
                parseGraphPosNode(sec.path("graphPos"), points);
                parsePointArray(sec.path("graphPos"), points);
            }
        }

        // 3) graphPos가 배열 객체 형태로 내려오는 케이스
        parsePointArray(laneNode.path("graphPos"), points);

        return points;
    }

    /**
     * graphPos 문자열 예시: "127.01,37.50 127.02,37.51"
     */
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
                // 좌표 한 점이 깨져 있어도 전체 파싱은 계속한다.
            }
        }
    }

    /**
     * graphPos가 [{"x":127.0,"y":37.5}, ...] 형태일 때 처리한다.
     */
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

