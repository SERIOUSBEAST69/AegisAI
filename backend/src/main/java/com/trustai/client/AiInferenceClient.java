package com.trustai.client;

import com.trustai.dto.ai.AiBatchClassificationRequest;
import com.trustai.dto.ai.AiBatchClassificationResponse;
import com.trustai.dto.ai.AiClassificationRequest;
import com.trustai.dto.ai.AiClassificationResult;
import com.trustai.dto.ai.RiskForecastRequest;
import com.trustai.dto.ai.RiskForecastResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "aiInferenceClient", url = "${ai.inference.base-url}")
public interface AiInferenceClient {

    @PostMapping("/predict")
    AiClassificationResult predict(@RequestBody AiClassificationRequest request);

    @PostMapping("/batch_predict")
    AiBatchClassificationResponse batchPredict(@RequestBody AiBatchClassificationRequest request);

    @PostMapping("/predict/risk")
    RiskForecastResponse predictRisk(@RequestBody RiskForecastRequest request);
}
