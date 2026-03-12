package com.trustai.controller;

import com.trustai.service.AiGatewayService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiGatewayController {

    @Autowired
    private AiGatewayService aiGatewayService;

    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody @Valid ChatReq req) {
        return R.ok(aiGatewayService.chat(req));
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
}
