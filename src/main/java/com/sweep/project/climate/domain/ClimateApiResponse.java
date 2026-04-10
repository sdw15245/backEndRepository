package com.sweep.project.climate.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ClimateApiResponse {

    private Response response;

    @Getter
    @NoArgsConstructor
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    public static class Body {
        private String dataType;
        private Items items;
        private int pageNo;
        private int numOfRows;
        private int totalCount;
    }

    @Getter
    @NoArgsConstructor
    public static class Items {
        private List<Item> item;
    }

    @Getter
    @NoArgsConstructor
    public static class Item {
        private String baseDate;
        private String baseTime;
        private ClimateCategory category;
        private String fcstDate;
        private String fcstTime;
        private String fcstValue;
        private int nx;
        private int ny;

        public String provideValue(){
            switch (category){
                //기온
                case TMP->{
                    return this.fcstValue+"℃";
                }
                //강수확률,상대습도
                case POP,REH->{
                    return this.fcstValue+"%";
                }
                //1시간 강수량
                case PCP->{
                    return this.fcstValue;
                }
                //적설량
                case SNO->{
                    if(this.fcstValue.equals("적설없음") || this.fcstValue.equals("1cm 미만")){
                        return this.fcstValue;
                    }
                    return this.fcstValue+"cm";
                }
                //그외엔 필요없어 보여서 걍 안넣음
                default -> {
                    return null;
                }
            }
        }
    }
}
