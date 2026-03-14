package com.trustai.controller;

import com.trustai.service.AiGatewayService;
import com.trustai.service.AiModelAccessGuardService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiGatewayController {

    @Autowired
    private AiGatewayService aiGatewayService;

    @Autowired
    private AiModelAccessGuardService aiModelAccessGuardService;

    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody @Valid ChatReq req) {
        return R.ok(aiGatewayService.chat(req));
    }

    /**
     * 隐私盾实时检测接口：前端在用户输入文本后调用此接口，
     * 在数据发送到任何 AI 服务之前检测是否含个人隐私信息。
     *
     * <p>前端可在输入框 debounce 后调用（500ms），若 detected 非空则展示红色警告横幅阻止发送。
     *
     * @param req 包含 text 字段（待检测文本）
     * @return {safe: bool, detected: [...字段类型], message: str}
     */
    @PostMapping("/privacy-check")
    public R<Map<String, Object>> privacyCheck(@RequestBody PrivacyCheckReq req) {
        List<String> detected = aiModelAccessGuardService.detectPrivacyFields(req.getText());
        Map<String, Object> result = new HashMap<>();
        result.put("safe", detected.isEmpty());
        result.put("detected", detected);
        if (!detected.isEmpty()) {
            result.put("message",
                    "⚠️ 检测到输入中含个人隐私信息（" + String.join("、", detected) +
                    "），已被 AegisAI 隐私盾拦截，禁止发送给 AI 模型。");
        } else {
            result.put("message", "✅ 未检测到个人隐私信息，可安全发送。");
        }
        return R.ok(result);
    }

    public static class ChatReq {
        @NotBlank
        private String provider; // qwen / qianfan / hunyuan / spark / doubao / yiyan
        @NotBlank
        private String model;
        @NotEmpty
        private List<Message> messages;
        private Long assetId; // optional data asset association
        private String accessReason;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
        public Long getAssetId() { return assetId; }
        public void setAssetId(Long assetId) { this.assetId = assetId; }
        public String getAccessReason() { return accessReason; }
        public void setAccessReason(String accessReason) { this.accessReason = accessReason; }
    }

    public static class Message {
        @NotBlank
        private String role; // user/assistant/system
        @NotBlank
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class PrivacyCheckReq {
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}

