package com.trustai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustai.client.AiInferenceClient;
import com.trustai.controller.AiGatewayController.BattleReq;
import com.trustai.controller.AiGatewayController.ChatReq;
import com.trustai.controller.AiGatewayController.Message;
import com.trustai.entity.AiCallLog;
import com.trustai.entity.AiModel;
import com.trustai.entity.AuditLog;
import com.trustai.entity.RiskEvent;
import com.trustai.entity.SecurityEvent;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
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
    private final SecurityEventService securityEventService;
    private final RiskEventService riskEventService;
    private final AuditLogService auditLogService;
    private final CompanyScopeService companyScopeService;

    public AiGatewayService(AiCallAuditService aiCallAuditService,
                            AiModelService aiModelService,
                            AiModelAccessGuardService aiModelAccessGuardService,
                            AiInferenceClient aiInferenceClient,
                            SecurityEventService securityEventService,
                            RiskEventService riskEventService,
                            AuditLogService auditLogService,
                            CompanyScopeService companyScopeService) {
        this.aiCallAuditService = aiCallAuditService;
        this.aiModelService = aiModelService;
        this.aiModelAccessGuardService = aiModelAccessGuardService;
        this.aiInferenceClient = aiInferenceClient;
        this.securityEventService = securityEventService;
        this.riskEventService = riskEventService;
        this.auditLogService = auditLogService;
        this.companyScopeService = companyScopeService;
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

    public List<Map<String, Object>> modelCatalog() {
        return aiModelService.lambdaQuery()
            .eq(AiModel::getStatus, "enabled")
            .list()
            .stream()
            .map(item -> {
                Map<String, Object> model = new HashMap<>();
                model.put("id", item.getId());
                model.put("modelCode", item.getModelCode());
                model.put("modelName", item.getModelName());
                model.put("provider", item.getProvider());
                model.put("riskLevel", item.getRiskLevel());
                model.put("status", item.getStatus());
                return model;
            })
            .collect(Collectors.toList());
    }

    public Map<String, Object> adversarialMeta() {
        Map<String, Object> assessment = buildThreatAssessment(false);
        try {
            Map<String, Object> meta = aiInferenceClient.adversarialMeta();
            if (meta != null) {
                assessment.putAll(meta);
            }
            assessment.put("adversarialAvailable", true);
        } catch (Exception ex) {
            assessment.put("adversarialAvailable", false);
            assessment.put("adversarialError", ex.getMessage());
            assessment.putIfAbsent("scenarios", List.of(
                Map.of("code", "real-threat-check", "description", "基于当前真实日志做实时态势评估")
            ));
        }
        return assessment;
    }

    public Map<String, Object> adversarialRun(BattleReq req) {
        String scenario = req == null || req.getScenario() == null || req.getScenario().isBlank()
            ? "real-threat-check"
            : req.getScenario().trim();
        int rounds = req == null || req.getRounds() == null ? 10 : Math.max(1, Math.min(100, req.getRounds()));
        Integer seed = req == null ? null : req.getSeed();

        Map<String, Object> assessment = buildThreatAssessment(true);
        if ("real-threat-check".equalsIgnoreCase(scenario)) {
            assessment.put("ok", true);
            assessment.put("mode", "real-threat-assessment");
            assessment.put("scenario", "real-threat-check");
            return assessment;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("scenario", scenario);
            payload.put("rounds", rounds);
            if (seed != null) {
                payload.put("seed", seed);
            }
            Map<String, Object> remote = aiInferenceClient.adversarialRun(payload);
            Map<String, Object> merged = new HashMap<>(assessment);
            if (remote != null) {
                merged.putAll(remote);
            }
            merged.put("assessment", assessment);
            merged.putIfAbsent("ok", true);
            return merged;
        } catch (Exception ex) {
            Map<String, Object> fallback = new HashMap<>(assessment);
            fallback.put("ok", false);
            fallback.put("error", "攻防引擎暂不可用: " + ex.getMessage());
            return fallback;
        }
    }

    private Map<String, Object> buildThreatAssessment(boolean immediateCheck) {
        Long companyId = companyScopeService.requireCompanyId();
        Date oneDayAgo = Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        long securityCritical = securityEventService.count(new QueryWrapper<SecurityEvent>()
            .eq("company_id", companyId)
            .in("severity", List.of("critical", "high"))
            .in("status", List.of("pending", "reviewing")));
        long securityPending = securityEventService.count(new QueryWrapper<SecurityEvent>()
            .eq("company_id", companyId)
            .eq("status", "pending"));
        long openRisk = riskEventService.count(new QueryWrapper<RiskEvent>()
            .eq("company_id", companyId)
            .in("status", List.of("open", "processing")));
        List<Long> userIds = companyScopeService.companyUserIds();
        long highRiskAudit = userIds.isEmpty() ? 0L : auditLogService.count(new QueryWrapper<AuditLog>()
            .in("user_id", userIds)
            .in("risk_level", List.of("HIGH", "high", "MEDIUM", "medium"))
            .ge("operation_time", oneDayAgo));

        List<SecurityEvent> latestEvents = securityEventService.list(new QueryWrapper<SecurityEvent>()
            .eq("company_id", companyId)
            .orderByDesc("event_time")
            .last("limit 8"));

        long weightedPressure = securityCritical * 4 + securityPending * 2 + openRisk * 3 + highRiskAudit;
        long normalized = Math.min(100, weightedPressure * 3);
        String level = normalized >= 70 ? "high" : (normalized >= 40 ? "medium" : "low");

        Map<String, Object> assessment = new HashMap<>();
        assessment.put("companyId", companyId);
        assessment.put("mode", "real-threat-assessment");
        assessment.put("immediateCheck", immediateCheck);
        assessment.put("threatLevel", level);
        assessment.put("riskScore", normalized);
        assessment.put("checkedAt", System.currentTimeMillis());
        assessment.put("signals", Map.of(
            "securityCritical", securityCritical,
            "securityPending", securityPending,
            "openRiskEvents", openRisk,
            "highRiskAudit24h", highRiskAudit
        ));
        assessment.put("recentSecurityEvents", latestEvents);
        return assessment;
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
