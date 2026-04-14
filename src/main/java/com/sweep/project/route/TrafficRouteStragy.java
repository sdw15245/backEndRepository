package com.sweep.project.route;

import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.dto.RequestRouteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Strategy Context.
 * PathSearchType에 따라 적절한 {@link AbstractRouteSearch} 전략을 선택하여 위임한다.
 *
 * <pre>
 * 사용 예:
 *   List&lt;? extends TrafficResponse&gt; routes = stragy.getRoutes(PATH_TYPE_SUBWAY);
 *   BoardingInfo info = stragy.getBoardingInfo(PATH_TYPE_SUBWAY, arrivalTime, routes.get(0));
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficRouteStragy {

    private final List<AbstractRouteSearch> strategies;

    /**
     * 경로 목록을 조회한다.
     *
     * @return SubwayRoute 또는 BusRoute 의 리스트
     * @throws IllegalArgumentException 해당 타입을 처리하는 전략이 없는 경우
     */
    public List<? extends TrafficResponse> getRoutes(PathSearchType pathSearchType,double startLat,double startLon
            ,double endLat,double endLon) {
        return findStrategy(pathSearchType).getRoutes(pathSearchType,startLat,startLon,endLat,endLon);
    }

    /**
     * 경로 목록 전체의 탑승 정보를 계산한다.
     *
     * @param pathSearchType     교통 수단 유형 (전략 선택에 사용)
     * @param desiredArrivalTime 목적지 도착 희망 일시
     * @param routes             {@link #getRoutes}로 얻은 경로 목록
     * @return 각 경로에 대한 BoardingInfo 리스트
     */
    public List<BoardingInfo> getBoardingInfo(PathSearchType pathSearchType,
                                              LocalDateTime desiredArrivalTime,
                                              List<? extends TrafficResponse> routes) {
        AbstractRouteSearch strategy = findStrategy(pathSearchType);
        return routes.stream()
                .map(route -> strategy.getBoardingInfo(desiredArrivalTime, route))
                .toList();
    }

    private AbstractRouteSearch findStrategy(PathSearchType pathSearchType) {
        return strategies.stream()
                .filter(s -> s.checkType(pathSearchType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "처리 가능한 전략이 없습니다: " + pathSearchType));
    }
}
