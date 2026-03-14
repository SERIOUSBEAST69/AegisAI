package com.trustai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustai.client.AiInferenceClient;
import com.trustai.controller.AiGatewayController.ChatReq;
import com.trustai.controller.AiGatewayController.Message;
import com.trustai.entity.AiCallLog;
import com.trustai.entity.AiModel;
import com.trustai.service.AiCallAuditService;
import com.trustai.service.AiModelService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiGatewayService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final AiCallAuditService aiCallAuditService;
    private final AiModelService aiModelService;
    private final AiModelAccessGuardService aiModelAccessGuardService;
    private final AiInferenceClient aiInferenceClient;

    public AiGatewayService(AiCallAuditService aiCallAuditService,
                            AiModelService aiModelService,
                            AiModelAccessGuardService aiModelAccessGuardService,
                            AiInferenceClient aiInferenceClient) {
        this.aiCallAuditService = aiCallAuditService;
        this.aiModelService = aiModelService;
        this.aiModelAccessGuardService = aiModelAccessGuardService;
        this.aiInferenceClient = aiInferenceClient;
    }

    public Map<String, Object> modelMetrics() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> metrics = aiInferenceClient.metrics();
            result.put("available", true);
            result.put("fetchedAt", System.currentTimeMillis());
            result.put("metrics", metrics);
        } catch (Exception ex) {
            result.put("available", false);
            result.put("reason", "PYTHON_SERVICE_UNAVAILABLE");
            result.put("message", ex.getMessage());
            result.put("metrics", Map.of());
        }
        return result;
    }

    public Map<String, Object> chat(ChatReq req) {
        Instant begin = Instant.now();
        AiModel model = aiModelService.lambdaQuery().eq(AiModel::getModelCode, req.getModel()).one();
        aiModelAccessGuardService.validate(model, req.getAssetId(), req.getAccessReason(), mergeMessages(req.getMessages()));

        String provider = model.getProvider() != null ? model.getProvider().toLowerCase(Locale.ROOT) : req.getProvider().toLowerCase(Locale.ROOT);
        ProviderConfig cfg = ProviderConfig.of(provider);
        String statusFlag = "success";
        if (cfg == null) {
            Map<String, Object> mock = mock("unsupported provider: " + provider, req.getMessages());
            persistLog(req, model, provider, mock, begin, "fail");
            return mock;
        }
        if (cfg.apiKey == null || cfg.apiKey.isEmpty()) {
            Map<String, Object> mock = mock("apiKey missing for " + provider + " (返回模拟响应)", req.getMessages());
            persistLog(req, model, provider, mock, begin, "fail");
            return mock;
        }

        try {
            String body = cfg.payloadBuilder.build(req.getModel(), req.getMessages());
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(cfg.url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json");
            cfg.authApplier.apply(builder, cfg.apiKey);
            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // ── 响应泄露扫描：检测 AI 是否将用户隐私原样回传 ───────────────────
            String rawBody = aiModelAccessGuardService.scanResponseForExfiltration(resp.body(), req.getModel());
            Map<String, Object> map = new HashMap<>();
            map.put("provider", provider);
            map.put("model", req.getModel());
            map.put("status", resp.statusCode());
            map.put("raw", rawBody);
            statusFlag = resp.statusCode() >= 200 && resp.statusCode() < 300 ? "success" : "fail";
            persistLog(req, model, provider, map, begin, statusFlag);
            return map;
        } catch (Exception e) {
            Map<String, Object> mock = mock("invoke failed: " + e.getMessage(), req.getMessages());
            persistLog(req, model, provider, mock, begin, "fail");
            return mock;
        }
    }

    private Map<String, Object> mock(String reason, List<Message> messages) {
        Map<String, Object> map = new HashMap<>();
        map.put("provider", "mock");
        map.put("reply", "【模拟响应】" + (messages.isEmpty() ? "" : messages.get(messages.size()-1).getContent()));
        map.put("reason", reason);
        return map;
    }

    private void persistLog(ChatReq req, AiModel model, String provider, Map<String, Object> resp, Instant begin, String status) {
        try {
            AiCallLog log = new AiCallLog();
            log.setModelCode(req.getModel());
            if (model != null) log.setModelId(model.getId());
            log.setProvider(provider);
            String inputPreview = req.getMessages() == null || req.getMessages().isEmpty()
                    ? "" : req.getMessages().get(req.getMessages().size() - 1).getContent();
            if (inputPreview != null && inputPreview.length() > 100) inputPreview = inputPreview.substring(0, 100);
            log.setInputPreview(inputPreview);
            String outputPreview = null;
            Object reply = resp.get("reply") != null ? resp.get("reply") : resp.get("raw");
            if (reply != null) {
                outputPreview = reply.toString();
                if (outputPreview.length() > 100) outputPreview = outputPreview.substring(0, 100);
            }
            log.setOutputPreview(outputPreview);
            log.setStatus(status);
            log.setErrorMsg("fail".equals(status) ? String.valueOf(resp.get("reason")) : null);
            log.setDurationMs(Duration.between(begin, Instant.now()).toMillis());
            log.setDataAssetId(req.getAssetId());
            log.setCreateTime(LocalDateTime.now());
            aiCallAuditService.recordAsync(log);
        } catch (Exception ignored) {
            // 审计失败不影响主流程
        }
    }

    private String mergeMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return messages.stream()
                .map(Message::getContent)
                .collect(Collectors.joining(" "));
    }

    private interface PayloadBuilder { String build(String model, List<Message> messages) throws Exception; }
    private interface AuthApplier { void apply(HttpRequest.Builder builder, String apiKey); }

    private static class ProviderConfig {
        final String url;
        final String apiKey;
        final PayloadBuilder payloadBuilder;
        final AuthApplier authApplier;

        ProviderConfig(String url, String apiKey, PayloadBuilder payloadBuilder, AuthApplier authApplier) {
            this.url = url;
            this.apiKey = apiKey;
            this.payloadBuilder = payloadBuilder;
            this.authApplier = authApplier;
        }

        static ProviderConfig of(String provider) {
            switch (provider) {
                case "qwen":
                    return new ProviderConfig(
                            "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
                            System.getenv("ALI_DASHSCOPE_API_KEY"),
                            (model, msgs) -> MAPPER.writeValueAsString(Map.of(
                                    "model", model,
                                    "input", Map.of("messages", msgs.stream().map(m -> Map.of("role", m.getRole(), "content", m.getContent())).collect(Collectors.toList())))),
                            (b, key) -> b.header("Authorization", "Bearer " + key)
                    );
                case "qianfan":
                case "wenxin":
                case "yiyan":
                    return new ProviderConfig(
                            "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions",
                            System.getenv("BAIDU_API_KEY"),
                            (model, msgs) -> MAPPER.writeValueAsString(Map.of(
                                    "model", model,
                                    "messages", msgs.stream().map(m -> Map.of("role", m.getRole(), "content", m.getContent())).collect(Collectors.toList()))),
                            (b, key) -> b.uri(URI.create("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token=" + key))
                    );
                case "hunyuan":
                    return new ProviderConfig(
                            "https://api.hunyuan.cloud.tencent.com/v1/chat/completions",
                            System.getenv("TENCENT_HUNYUAN_API_KEY"),
                            (model, msgs) -> MAPPER.writeValueAsString(Map.of(
                                    "model", model,
                                    "messages", msgs.stream().map(m -> Map.of("role", m.getRole(), "content", m.getContent())).collect(Collectors.toList()))),
                            (b, key) -> b.header("Authorization", "Bearer " + key)
                    );
                case "spark":
                case "xinghuo":
                    return new ProviderConfig(
                            "https://spark-api.xf-yun.com/v3.5/chat",
                            System.getenv("IFLYTEK_SPARK_API_KEY"),
                            (model, msgs) -> MAPPER.writeValueAsString(Map.of(
                                    "model", model,
                                    "messages", msgs.stream().map(m -> Map.of("role", m.getRole(), "content", m.getContent())).collect(Collectors.toList()))),
                            (b, key) -> b.header("Authorization", key)
                    );
                case "doubao":
                    return new ProviderConfig(
                            "https://ark.cn-beijing.volces.com/api/v1/chat/completions",
                            System.getenv("BYTE_DOUYIN_API_KEY"),
                            (model, msgs) -> MAPPER.writeValueAsString(Map.of(
                                    "model", model,
                                    "messages", msgs.stream().map(m -> Map.of("role", m.getRole(), "content", m.getContent())).collect(Collectors.toList()))),
                            (b, key) -> b.header("Authorization", "Bearer " + key)
                    );
                default:
                    return null;
            }
        }
    }
}
