package com.trustai.service;

import com.trustai.dto.ai.RiskForecastResponse;

public interface RiskPredictionService {
    RiskForecastResponse forecastNext7Days();
}
