package com.sweep.project.route;

import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.dto.RequestRouteDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
@Slf4j
public abstract class AbstractRouteSearch {

    @Value("${api-key.odsay}")
    protected String odsayKey;

    @Value("${api-key.korea-data-portal}")
    protected String seoulBusApiKey;

    protected static final String ROUTE_SEARCH_URL = "https://api.odsay.com/v1/api/searchPubTransPathT";
    protected static final String SCHEDULE_SEARCH_URL = "https://api.odsay.com/v1/api/subwayPathSchedule";
    protected static final String BUS_ARRIVAL_URL = "http://ws.bus.go.kr/api/rest/arrive/getArrInfoByRoute";

    private static final String START_X = "126.926493082645";
    private static final String START_Y = "37.6134436427887";
    private static final String END_X = "127.126936754911";
    private static final String END_Y = "37.5004198786564";

    protected final RestTemplate restTemplate = new RestTemplate();

    /** 이 전략이 주어진 타입을 처리할 수 있는지 여부 */
    public abstract boolean checkType(PathSearchType pathSearchType);

    /**
     * 경로 목록을 조회한다.
     * 반환 타입은 구체 클래스에 따라 List&lt;SubwayRoute&gt; 또는 List&lt;BusRoute&gt;.
     */
    public abstract List<? extends TrafficResponse> getRoutes(PathSearchType type,double startLat,double startLon
            ,double endLat,double endLon);

    /**
     * 탑승 정보를 계산한다.
     *
     * @param desiredArrivalTime 목적지 도착 희망 일시
     * @param route              {@link #getRoutes}로 얻은 단일 경로
     */
    public abstract BoardingInfo getBoardingInfo(LocalDateTime desiredArrivalTime, TrafficResponse route);


    protected OdsayRouteResponse callRouteApi(int searchPathType,
                                              double startLat,double startLon
            ,double endLat,double endLon) {
        log.info("searchType:{}",searchPathType);
        String url = UriComponentsBuilder.fromHttpUrl(ROUTE_SEARCH_URL)
                .queryParam("lang",0)
                .queryParam("apiKey", odsayKey)
                .queryParam("SX", startLat)
                .queryParam("SY", startLon)
                .queryParam("EX", endLat)
                .queryParam("EY",endLon)
                //.queryParam("SearchType",0)
                .queryParam("SearchPathType", searchPathType)
                .queryParam("output","json")
                .toUriString();
        log.info("url:{}",url);
        return restTemplate.getForObject(url, OdsayRouteResponse.class);
    }
}
