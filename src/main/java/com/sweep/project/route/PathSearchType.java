package com.sweep.project.route;

public enum PathSearchType {

    PATH_TYPE_ANYONE(0),PATH_TYPE_SUBWAY(1),PATH_TYPE_BUS(2);


    public int pathType;

    PathSearchType(int pathType) {
        this.pathType=pathType;
    }


}
