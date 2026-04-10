package com.sweep.project.climate.service;

import com.sweep.project.climate.ClimateAop;
import com.sweep.project.climate.domain.ClimateApiResponse;
import com.sweep.project.climate.domain.ClimateInfo;
import com.sweep.project.redis.RedisClimateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sweep.project.climate.ClimateGeoConverter.latLonToGrid;
import static com.sweep.project.climate.ClimateGeoConverter.searchTimeConverter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClimateService {

    @Value("${climate.api-key}")
    private String climateApiKey;

    @Value("${climate.api-url}")
    private String climateApiUrl;

    private final RestTemplate restTemplate;

    private final RedisClimateService redisClimateService;

    // 기상청 발표 시각 (정수 비교용: "0200" → 200)
    private static final int[] BASE_TIMES = {200, 500, 800, 1100, 1400, 1700, 2000, 2300};


    @ClimateAop
    public List<ClimateInfo> getData(double lat, double lon, LocalDateTime arrivalTime){
        int[] nxNy = latLonToGrid(lat, lon);

        String baseDate = String.format("%d%02d%02d",
                arrivalTime.getYear(),
                arrivalTime.getMonthValue(),
                arrivalTime.getDayOfMonth());
        String baseTime = String.format("%02d00", arrivalTime.getHour());

        List<ClimateApiResponse.Item> data=
                redisClimateService.getClimateInfoByTime(nxNy,baseDate+"_"+baseTime);

        if(data.isEmpty()){
            Map<String, List<ClimateApiResponse.Item>> parsedData=parseData(nxNy,arrivalTime);
            redisClimateService.saveClimateInfo(nxNy,parsedData);
            return parsedData.get(baseDate+"_"+baseTime)
                    .stream().map(x->{
                        return new ClimateInfo(x.getCategory(),x.provideValue());
                    }).toList();
        }
        return data.stream().map(x->{
            return new ClimateInfo(x.getCategory(),x.provideValue());
        }).toList();
    }


    private Map<String, List<ClimateApiResponse.Item>> parseData(int[] nxNy, LocalDateTime arrivalTime) {
        String[] baseDatetime = searchTimeConverter(arrivalTime);

        log.info("args:{}-{}-{}-{}",nxNy[0],nxNy[1],baseDatetime[0],baseDatetime[1]);

        String url = UriComponentsBuilder.fromHttpUrl(climateApiUrl)
                .queryParam("authKey", climateApiKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 36)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDatetime[0])
                .queryParam("base_time", baseDatetime[1])
                .queryParam("nx", nxNy[0])
                .queryParam("ny", nxNy[1])
                .build(true)  // 이미 인코딩된 authKey 재인코딩 방지
                .toUriString();

        ClimateApiResponse apiResponse = restTemplate.getForObject(url, ClimateApiResponse.class);

        if (apiResponse == null
                || apiResponse.getResponse() == null
                || !"00".equals(apiResponse.getResponse().getHeader().getResultCode())) {
            throw new RuntimeException("기후 API 호출 실패: "
                    + (apiResponse != null && apiResponse.getResponse() != null
                    ? apiResponse.getResponse().getHeader().getResultMsg()
                    : "응답 없음"));
        }

        return filterByBaseWindow(
                apiResponse.getResponse().getBody().getItems().getItem(),
                baseDatetime[0],
                baseDatetime[1]
        );
    }

    /**
     * baseTime ~ 다음 baseTime 직전 구간의 예보만 추출 후 (fcstDate, fcstTime) 기준으로 그룹핑.
     * 키 형식: "fcstDate_fcstTime" (e.g. "20260410_1500")
     * 23시 구간은 당일 2300 이후 + 익일 0200 미만을 함께 처리.
     */
    private Map<String, List<ClimateApiResponse.Item>> filterByBaseWindow(
            List<ClimateApiResponse.Item> items,
            String baseDate,
            String baseTime) {

        int baseTimeInt = Integer.parseInt(baseTime);  // e.g. "1400" → 1400

        // 다음 발표 시각 및 날짜 계산
        int nextBaseTimeInt;
        String nextDate;

        int idx = -1;
        for (int i = 0; i < BASE_TIMES.length; i++) {
            if (BASE_TIMES[i] == baseTimeInt) {
                idx = i;
                break;
            }
        }

        if (idx >= 0 && idx < BASE_TIMES.length - 1) {
            // 일반 구간: 다음 발표 시각은 같은 날
            nextBaseTimeInt = BASE_TIMES[idx + 1];
            nextDate = baseDate;
        } else {
            // 2300 구간: 다음 발표는 익일 0200
            nextBaseTimeInt = BASE_TIMES[0]; // 200
            nextDate = incrementDate(baseDate);
        }

        final int finalNext = nextBaseTimeInt;
        final String finalNextDate = nextDate;

        return items.stream()
                .filter(item -> {
                    int fcstTimeInt = Integer.parseInt(item.getFcstTime());
                    String fcstDate = item.getFcstDate();

                    if (baseTimeInt < 2300) {
                        // 일반 구간: 같은 날, baseTime 이상 nextBaseTime 미만
                        return fcstDate.equals(baseDate)
                                && fcstTimeInt >= baseTimeInt
                                && finalNext>=fcstTimeInt ;
                    } else {
                        // 2300 구간: 당일 2300 이후 OR 익일 0200(200) 미만
                        return (fcstDate.equals(baseDate) && fcstTimeInt >= 2300)
                                || (fcstDate.equals(finalNextDate) && fcstTimeInt < 200);
                    }
                })
                .collect(Collectors.groupingBy(item -> item.getFcstDate() + "_" + item.getFcstTime()));
    }

    private String incrementDate(String date) {
        // "YYYYMMDD" 형식으로 +1일
        LocalDate next = LocalDate.of(
                Integer.parseInt(date.substring(0, 4)),
                Integer.parseInt(date.substring(4, 6)),
                Integer.parseInt(date.substring(6, 8))
        ).plusDays(1);
        return String.format("%d%02d%02d", next.getYear(), next.getMonthValue(), next.getDayOfMonth());
    }
}
