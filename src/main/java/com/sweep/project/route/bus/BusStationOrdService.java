package com.sweep.project.route.bus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.redis.RouteRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.OptionalInt;

/**
 * 버스 정류소 순번(ord) 조회 서비스.
 *
 * <p>Redis를 1차 캐시로 사용하며, 미스 시 지역 BIS API를 호출하여 ord를 조회한다.
 * ord가 정상 조회된 경우 Redis에 저장(TTL 7일)한다.</p>
 *
 * <h3>키 구조</h3>
 * <pre>bus:ord:{providerCode}:{busRouteId}:{stId}</pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusStationOrdService {

    private final RouteRedisService routeRedisService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api-key.korea-data-portal}")
    private String busApiKey;

    @Value("${bus.api.station-ord.gbis-url}")
    private String gbisStationOrdUrl;

    /**
     * providerCode + busRouteId + stId 조합으로 ord(정류소 순번)를 반환한다.
     *
     * <ol>
     *   <li>Redis 캐시 조회</li>
     *   <li>미스 시 지역 BIS API 호출</li>
     *   <li>조회 성공 시 Redis 저장</li>
     * </ol>
     *
     * @param providerCode 버스 제공자 코드 (2: 경기도, 4: 서울)
     * @param busRouteId   버스 노선 ID
     * @param stId         정류소 ID
     * @return ord 값 (조회 실패 시 0)
     */
    public int resolveOrd(int providerCode, String busRouteId, String stId) {
        OptionalInt cached = routeRedisService.getOrd(providerCode, busRouteId, stId);
        if (cached.isPresent()) {
            return cached.getAsInt();
        }

        int ord = fetchOrd(providerCode, busRouteId, stId);
        if (ord != 0) {
            routeRedisService.saveOrd(providerCode, busRouteId, stId, ord);
        }
        return ord;
    }

    private int fetchOrd(int providerCode, String busRouteId, String stId) {
        return switch (providerCode) {
            case 2  -> fetchGbisOrd(busRouteId, stId);
            default -> fetchSeoulOrd(busRouteId, stId);
        };
    }

    private int fetchGbisOrd(String routeId, String stationId) {
        String url = UriComponentsBuilder.fromHttpUrl(gbisStationOrdUrl)
                .queryParam("serviceKey", busApiKey)
                .queryParam("routeId", routeId)
                .queryParam("format", "json")
                .build(false)
                .toUriString();
        log.info("[BusStationOrd] 경기도 순번 조회 url={}", url);
        try {
            String json = restTemplate.getForObject(URI.create(url), String.class);
            JsonNode list = objectMapper.readTree(json)
                    .path("response").path("msgBody").path("busRouteStationList");
            if (list.isArray()) {
                for (JsonNode station : list) {
                    if (stationId.equals(station.path("stationId").asText())) {
                        int ord = station.path("stationSeq").asInt(0);
                        log.info("[BusStationOrd] 경기도 ord={} routeId={} stationId={}", ord, routeId, stationId);
                        return ord;
                    }
                }
            }
            log.warn("[BusStationOrd] 경기도 순번 못 찾음 routeId={} stationId={}", routeId, stationId);
            return 0;
        } catch (Exception e) {
            log.error("[BusStationOrd] 경기도 순번 조회 실패", e);
            return 0;
        }
    }

    private int fetchSeoulOrd(String routeId, String stationId) {
        log.warn("[BusStationOrd] 서울 순번 자동조회 미구현 routeId={} stationId={}", routeId, stationId);
        return 0;
    }
}
