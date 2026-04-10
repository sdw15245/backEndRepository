package com.sweep.project.climate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class ClimateGeoConverter {

    // 기상청 Lambert Conformal Conic 투영 상수
    static final double RE = 6371.00877;   // 지구 반경 (km)
    static final double GRID = 5.0;        // 격자 간격 (km)
    static final double SLAT1 = 30.0;     // 투영 위도1 (degree)
    static final double SLAT2 = 60.0;     // 투영 위도2 (degree)
    static final double OLON = 126.0;     // 기준점 경도 (degree)
    static final double OLAT = 38.0;      // 기준점 위도 (degree)
    static final double XO = 43;          // 기준점 X 좌표 (격자)
    static final double YO = 136;         // 기준점 Y 좌표 (격자)

    public static int[] latLonToGrid(double lat, double lon) {
        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5)
                / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new int[]{nx, ny};
    }

    public static String[] searchTimeConverter(LocalDateTime arrivalTime){

        int hour = arrivalTime.getHour();
        int minute =arrivalTime.getMinute();

        // 발표 후 10분 지나야 데이터 준비됨
        int[] baseTimes = {2, 5, 8, 11, 14, 17, 20, 23};
        int baseHour = baseTimes[0];


        for (int bt : baseTimes) {
            if (hour > bt || (hour == bt && minute >= 10)) {
                baseHour = bt;
            }
        }

        // base_time이 자정 이전인 경우 날짜 처리
        LocalDateTime baseDateTime = arrivalTime;
        if (hour < 2 || (hour == 2 && minute < 10)) {
            // 아직 0200 데이터가 안 나왔으면 전날 2300으로
            baseDateTime = arrivalTime.minusDays(1);
            baseHour = 23;
        }

        String baseDate = String.format("%d%02d%02d",
                baseDateTime.getYear(),
                baseDateTime.getMonthValue(),
                baseDateTime.getDayOfMonth());

        String baseTime = String.format("%02d00", baseHour);

        return new String[]{baseDate, baseTime};
    }
}
