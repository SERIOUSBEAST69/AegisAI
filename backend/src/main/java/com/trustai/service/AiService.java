package com.trustai.service;

import com.trustai.dto.ai.AiCallRequest;
import com.trustai.dto.ai.AiCallResponse;
import com.trustai.dto.ai.AiMessage;
import com.trustai.entity.AiCallLog;
import com.trustai.entity.AiModel;
import com.trustai.utils.AesEncryptor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiModelService aiModelService;
    private final RateLimiterService rateLimiterService;
    private final AesEncryptor aesEncryptor;
    private final AiCallAuditService aiCallAuditService;
    private final WebClient.Builder webClientBuilder;

    public AiCallResponse chat(AiCallRequest request, Long userId, String ip) {
        AiModel model = aiModelService.lambdaQuery()
                .eq(AiModel::getModelCode, request.getModelCode())
                .eq(AiModel::getStatus, "enabled")
                .one();
        if (model == null) {
            throw new IllegalArgumentException("未找到可用模型:" + request.getModelCode());
        }
        String apiKey = aesEncryptor.decrypt(model.getApiKey());
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("模型未配置密钥");
        }

        rateLimiterService.checkQuota(model.getModelCode(), model.getCallLimit() == null ? 0 : model.getCallLimit(), LocalDate.now());
        Instant begin = Instant.now();
        String content;
        Integer tokens = null;
        try {
            if (isWenxin(model.getProvider())) {
                content = callWenxin(model, request, apiKey);
            } else {
                content = callOpenAiLike(model, request, apiKey);
            }
            rateLimiterService.increment(model.getModelCode(), LocalDate.now());
            AiCallLog logEntry = buildLog(model, userId, ip, request, content, "success", null, Duration.between(begin, Instant.now()).toMillis(), tokens);
            aiCallAuditService.recordAsync(logEntry);
            return new AiCallResponse(content, tokens, Duration.between(begin, Instant.now()).toMillis(), model.getProvider(), model.getModelName());
        } catch (Exception e) {
            AiCallLog logEntry = buildLog(model, userId, ip, request, null, "fail", e.getMessage(), Duration.between(begin, Instant.now()).toMillis(), tokens);
            aiCallAuditService.recordAsync(logEntry);
            throw new IllegalStateException("模型调用失败:" + e.getMessage(), e);
        }
    }

    private String callOpenAiLike(AiModel model, AiCallRequest request, String apiKey) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(model.getApiUrl())
                .apiKey(apiKey)
                .build();
        OpenAiChatModel chatModel = new OpenAiChatModel(api);
        ChatClient client = ChatClient.builder(chatModel).build();
        List<Message> messages = convertMessages(request.getMessages(), request.getPrompt());
        return client.prompt().messages(messages).call().content();
    }

    private String callWenxin(AiModel model, AiCallRequest request, String apiKey) {
        String[] parts = apiKey.split("\\|");
        if (parts.length != 2) {
            throw new IllegalStateException("文心模型需要以 clientId|clientSecret 形式配置密钥");
        }
        String token = fetchBaiduToken(parts[0], parts[1]);
        WebClient client = webClientBuilder.baseUrl(model.getApiUrl()).build();
        List<Message> messages = convertMessages(request.getMessages(), request.getPrompt());
        WenxinPayload payload = new WenxinPayload(model.getModelCode(), messages);
        return client.post()
                .uri(uriBuilder -> uriBuilder.queryParam("access_token", token).build())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(WenxinResponse.class)
                .blockOptional(Duration.ofSeconds(30))
                .map(WenxinResponse::firstResult)
                .orElseThrow(() -> new IllegalStateException("文心返回为空"));
    }

    private String fetchBaiduToken(String clientId, String clientSecret) {
        WebClient client = webClientBuilder.baseUrl("https://aip.baidubce.com").build();
        return client.get()
                .uri(uriBuilder -> uriBuilder.path("/oauth/2.0/token")
                        .queryParam("grant_type", "client_credentials")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .build())
                .retrieve()
                .bodyToMono(TokenResp.class)
                .blockOptional(Duration.ofSeconds(15))
                .map(TokenResp::getAccess_token)
                .orElseThrow(() -> new IllegalStateException("获取文心 access_token 失败"));
    }

    private List<Message> convertMessages(List<AiMessage> messages, String systemPrompt) {
        List<Message> list = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            list.add(new SystemMessage(systemPrompt));
        }
        if (!CollectionUtils.isEmpty(messages)) {
            for (AiMessage m : messages) {
                switch (m.getRole()) {
                    case "user":
                        list.add(new UserMessage(m.getContent()));
                        break;
                    case "assistant":
                        list.add(new AssistantMessage(m.getContent()));
                        break;
                    case "system":
                        list.add(new SystemMessage(m.getContent()));
                        break;
                    default:
                        list.add(new UserMessage(m.getContent()));
                }
            }
        }
        return list;
    }

    private boolean isWenxin(String provider) {
        if (provider == null) return false;
        String p = provider.toLowerCase();
        return Objects.equals(p, "wenxin") || Objects.equals(p, "qianfan") || Objects.equals(p, "yiyan");
    }

    private AiCallLog buildLog(AiModel model, Long userId, String ip, AiCallRequest req, String output, String status, String error, long durationMs, Integer tokens) {
        AiCallLog logEntry = new AiCallLog();
        logEntry.setUserId(userId);
        logEntry.setModelId(model.getId());
        logEntry.setModelCode(model.getModelCode());
        logEntry.setProvider(model.getProvider());
        String inputPreview = req.getMessages() == null || req.getMessages().isEmpty() ? "" : req.getMessages().get(req.getMessages().size() - 1).getContent();
        if (inputPreview != null && inputPreview.length() > 100) inputPreview = inputPreview.substring(0, 100);
        logEntry.setInputPreview(inputPreview);
        String outputPreview = output;
        if (outputPreview != null && outputPreview.length() > 100) outputPreview = outputPreview.substring(0, 100);
        logEntry.setOutputPreview(outputPreview);
        logEntry.setStatus(status);
        logEntry.setErrorMsg(error);
        logEntry.setDurationMs(durationMs);
        logEntry.setTokenUsage(tokens);
        logEntry.setIp(ip);
        logEntry.setCreateTime(java.time.LocalDateTime.now());
        return logEntry;
    }

    private record TokenResp(String access_token) {
        public String getAccess_token() { return access_token; }
    }

    private static class WenxinPayload {
        private final String model;
        private final List<Message> messages;
        WenxinPayload(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
        public String getModel() { return model; }
        public List<Message> getMessages() { return messages; }
    }

    private static class WenxinResponse {
        private List<Result> result;
        public List<Result> getResult() { return result; }
        public void setResult(List<Result> result) { this.result = result; }
        public String firstResult() {
            if (result == null || result.isEmpty()) return null;
            return result.get(0).content;
        }
        private static class Result { public String content; }
    }
}
