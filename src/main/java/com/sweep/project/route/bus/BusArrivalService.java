package com.sweep.project.route.bus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;

/**
 * 버스 도착 정보 조회 서비스.
 *
 * <p>providerCode에 따라 서울(4) 또는 경기도(2) BIS API를 호출하며,
 * ord가 0인 경우 {@link BusStationOrdService}를 통해 자동 조회한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusArrivalService {

    private final BusStationOrdService busStationOrdService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api-key.korea-data-portal}")
    private String busApiKey;

    @Value("${bus.api.arrival.seoul-url}")
    private String seoulArrivalUrl;

    @Value("${bus.api.arrival.gbis-url}")
    private String gbisArrivalUrl;

    /**
     * 버스 도착 정보를 조회한다.
     *
     * @param stId         정류소 ID
     * @param busRouteId   버스 노선 ID
     * @param ord          정류소 순번 (0이면 자동 조회)
     * @param providerCode 제공자 코드 (2: 경기도, 4: 서울)
     */
    public BusArrivalInfo getBusArrival(String stId, String busRouteId, int ord, int providerCode) {
        int resolvedOrd = (ord == 0) ? busStationOrdService.resolveOrd(providerCode, busRouteId, stId) : ord;
        log.info("[BusArrival] stId={} busRouteId={} ord={} providerCode={}", stId, busRouteId, resolvedOrd, providerCode);

        if (providerCode == 2) {
            return fetchGbisArrival(stId, busRouteId, resolvedOrd);
        }
        return fetchSeoulArrival(stId, busRouteId, resolvedOrd);
    }

    private BusArrivalInfo fetchGbisArrival(String stId, String busRouteId, int ord) {
        String url = UriComponentsBuilder.fromHttpUrl(gbisArrivalUrl)
                .queryParam("serviceKey", busApiKey)
                .queryParam("stationId", stId)
                .queryParam("routeId", busRouteId)
                .queryParam("staOrder", ord)
                .queryParam("format", "json")
                .build(false)
                .toUriString();
        log.info("[BusArrival] 경기도 url={}", url);
        return parseGbisArrivalJson(restTemplate.getForObject(URI.create(url), String.class));
    }

    private BusArrivalInfo fetchSeoulArrival(String stId, String busRouteId, int ord) {
        String url = UriComponentsBuilder.fromHttpUrl(seoulArrivalUrl)
                .queryParam("serviceKey", busApiKey)
                .queryParam("stId", stId)
                .queryParam("busRouteId", busRouteId)
                .queryParam("ord", ord)
                .build(false)
                .toUriString();
        log.info("[BusArrival] 서울 url={}", url);
        return parseBusArrivalXml(restTemplate.getForObject(URI.create(url), String.class));
    }

    private BusArrivalInfo parseGbisArrivalJson(String json) {
        log.info("[BusArrival] 경기도 원본 JSON={}", json);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode msgHeader = root.path("response").path("msgHeader");
            int resultCode = msgHeader.path("resultCode").asInt(-1);

            if (resultCode != 0) {
                String msg = msgHeader.path("resultMessage").asText("결과 없음");
                log.info("[BusArrival] 경기도 정보 없음: {}", msg);
                return new BusArrivalInfo(null, null, null, null, msg, 0, null, 0);
            }

            JsonNode item = root.path("response").path("msgBody").path("busArrivalItem");

            int predictTime1 = item.path("predictTime1").asInt(0);
            int locationNo1  = item.path("locationNo1").asInt(0);
            int predictTime2 = item.path("predictTime2").asInt(0);
            int locationNo2  = item.path("locationNo2").asInt(0);

            String arrmsg1 = predictTime1 > 0
                    ? predictTime1 + "분후[" + locationNo1 + "번째 전]"
                    : "운행 정보 없음";
            String arrmsg2 = predictTime2 > 0
                    ? predictTime2 + "분후[" + locationNo2 + "번째 전]"
                    : "운행 정보 없음";

            return new BusArrivalInfo(
                    item.path("stationId").asText(),
                    item.path("stationNm1").asText(),
                    item.path("routeId").asText(),
                    item.path("routeName").asText(),
                    arrmsg1,
                    item.path("predictTimeSec1").asInt(0),
                    arrmsg2,
                    item.path("predictTimeSec2").asInt(0)
            );
        } catch (Exception e) {
            log.error("[BusArrival] 경기도 JSON 파싱 실패", e);
            throw new RuntimeException("경기도 버스 도착 정보를 파싱할 수 없습니다: " + e.getMessage(), e);
        }
    }

    private BusArrivalInfo parseBusArrivalXml(String xml) {
        log.info("[BusArrival] 서울 원본 XML={}", xml);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            Element item = (Element) doc.getElementsByTagName("itemList").item(0);
            if (item == null) {
                String headerMsg = doc.getElementsByTagName("headerMsg").getLength() > 0
                        ? doc.getElementsByTagName("headerMsg").item(0).getTextContent().trim()
                        : "결과 없음";
                log.info("[BusArrival] 서울 정보 없음: {}", headerMsg);
                return new BusArrivalInfo(null, null, null, null, headerMsg, 0, null, 0);
            }

            return new BusArrivalInfo(
                    getText(item, "stId"),
                    getText(item, "stNm"),
                    getText(item, "busRouteId"),
                    getText(item, "rtNm"),
                    getText(item, "arrmsg1"),
                    parseIntSafe(getText(item, "traTime1")),
                    getText(item, "arrmsg2"),
                    parseIntSafe(getText(item, "traTime2"))
            );
        } catch (Exception e) {
            log.error("[BusArrival] 서울 XML 파싱 실패", e);
            throw new RuntimeException("버스 도착 정보를 파싱할 수 없습니다: " + e.getMessage(), e);
        }
    }

    private String getText(Element parent, String tagName) {
        var nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return "";
        return nodes.item(0).getTextContent();
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
