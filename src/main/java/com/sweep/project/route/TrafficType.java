package com.sweep.project.route;

public enum TrafficType {

    TRAFFIC_TYPE_SUBWAY(1),TRAFFIC_TYPE_BUS(2)
    ,TRAFFIC_TYPE_HUMAN(3);

    public int trafficNumber;

    TrafficType(int trafficNumber) {
        this.trafficNumber=trafficNumber;
    }

}
