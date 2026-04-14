package com.sweep.project.route.domain;

import java.util.Arrays;

public enum PathSearchType {

    PATH_TYPE_ANYONE(0),PATH_TYPE_SUBWAY(1),PATH_TYPE_BUS(2);


    public int pathType;

    PathSearchType(int pathType) {
        this.pathType=pathType;
    }



    public static PathSearchType from(int val){
        return Arrays.stream(PathSearchType.values())
                .filter(x -> x.pathType == val)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid value: " + val));
    }

}
