package com.trustai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustai.controller.AiGatewayController.ChatReq;
import com.trustai.controller.AiGatewayController.Message;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiGatewayService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Map<String, Object> chat(ChatReq req) {
        String provider = req.getProvider().toLowerCase();
        ProviderConfig cfg = ProviderConfig.of(provider);
        if (cfg == null) {
            return mock("unsupported provider: " + provider, req.getMessages());
        }
        if (cfg.apiKey == null || cfg.apiKey.isEmpty()) {
            return mock("apiKey missing for " + provider + " (返回模拟响应)", req.getMessages());
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
            Map<String, Object> map = new HashMap<>();
            map.put("provider", provider);
            map.put("model", req.getModel());
            map.put("status", resp.statusCode());
            map.put("raw", resp.body());
            return map;
        } catch (Exception e) {
            return mock("invoke failed: " + e.getMessage(), req.getMessages());
        }
    }

    private Map<String, Object> mock(String reason, List<Message> messages) {
        Map<String, Object> map = new HashMap<>();
        map.put("provider", "mock");
        map.put("reply", "【模拟响应】" + (messages.isEmpty() ? "" : messages.get(messages.size()-1).getContent()));
        map.put("reason", reason);
        return map;
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
