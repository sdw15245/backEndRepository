package com.sweep.project.climate.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClimateInfo {
    private ClimateCategory category;
    private String value;
}
