package com.trustai.dto.ai;

import java.util.List;
import lombok.Data;

@Data
public class RiskForecastResponse {
    private List<Double> forecast;
    private Integer trainingSamples;
    private Double trainingMae;
    private String method;
    private Boolean fallback;
}
