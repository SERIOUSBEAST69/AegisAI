package com.trustai.service;

import com.trustai.entity.AiModel;
import com.trustai.entity.DataAsset;
import com.trustai.entity.RiskEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiModelAccessGuardService {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(1\\d{10})|([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})|(\\d{15,18}[0-9Xx])|(身份证|银行卡|手机号|住址|邮箱|phone|email|bank|id card)",
            Pattern.CASE_INSENSITIVE
    );

    private final DataAssetService dataAssetService;
    private final RiskEventService riskEventService;

    public void validate(AiModel model, Long assetId, String accessReason, String inputText) {
        if (model == null) {
            block("UNREGISTERED_MODEL", "HIGH", null, assetId, "模型未注册，禁止绕过治理目录直接调用");
            throw new IllegalArgumentException("模型未注册，禁止绕过治理目录直接调用");
        }

        if (!isEnabled(model.getStatus())) {
            block("DISABLED_MODEL_CALL", "HIGH", model.getModelCode(), assetId, "模型已停用，不允许调用");
            throw new IllegalStateException("模型已停用，不允许调用");
        }

        DataAsset asset = null;
        if (assetId != null) {
            asset = dataAssetService.getById(assetId);
            if (asset == null) {
                block("INVALID_ASSET_BINDING", "MEDIUM", model.getModelCode(), assetId, "绑定的数据资产不存在");
                throw new IllegalArgumentException("绑定的数据资产不存在: " + assetId);
            }
        }

        boolean highRiskModel = isHighRisk(model.getRiskLevel());
        boolean mediumOrHighRisk = highRiskModel || isMediumRisk(model.getRiskLevel());
        boolean containsSensitiveInput = inputText != null && SENSITIVE_PATTERN.matcher(inputText).find();

        if (highRiskModel && assetId == null) {
            block("HIGH_RISK_MODEL_WITHOUT_ASSET", "HIGH", model.getModelCode(), null, "高风险模型调用必须绑定数据资产");
            throw new IllegalStateException("高风险模型调用必须绑定数据资产");
        }

        if (highRiskModel && (accessReason == null || accessReason.trim().length() < 6)) {
            block("HIGH_RISK_MODEL_WITHOUT_REASON", "HIGH", model.getModelCode(), assetId, "高风险模型调用必须填写不少于 6 个字的访问目的");
            throw new IllegalStateException("高风险模型调用必须填写不少于 6 个字的访问目的");
        }

        if (mediumOrHighRisk && containsSensitiveInput && assetId == null) {
            block("SENSITIVE_PROMPT_WITHOUT_ASSET", "HIGH", model.getModelCode(), null, "检测到疑似敏感内容，调用中高风险模型时必须绑定数据资产");
            throw new IllegalStateException("检测到疑似敏感内容，调用中高风险模型时必须绑定数据资产");
        }

        if (highRiskModel && asset != null && !isHighSensitivity(asset.getSensitivityLevel()) && accessReason != null && accessReason.length() < 10) {
            block("HIGH_RISK_MODEL_WEAK_REASON", "MEDIUM", model.getModelCode(), assetId, "高风险模型在非高敏资产场景下仍需更明确的调用理由");
            throw new IllegalStateException("高风险模型在当前场景下需要更明确的调用理由");
        }
    }

    private void block(String type, String level, String modelCode, Long assetId, String reason) {
        RiskEvent event = new RiskEvent();
        event.setType(type);
        event.setLevel(level);
        event.setStatus("open");
        event.setProcessLog("model:" + (modelCode == null ? "unknown" : modelCode)
                + " | asset:" + (assetId == null ? "none" : assetId)
                + " | " + reason);
        event.setCreateTime(new Date());
        event.setUpdateTime(new Date());
        riskEventService.save(event);
    }

    private boolean isEnabled(String status) {
        return status != null && "enabled".equalsIgnoreCase(status.trim());
    }

    private boolean isHighRisk(String riskLevel) {
        if (riskLevel == null) return false;
        String value = riskLevel.trim().toLowerCase(Locale.ROOT);
        return value.contains("high") || value.contains("critical") || value.contains("高");
    }

    private boolean isMediumRisk(String riskLevel) {
        if (riskLevel == null) return false;
        String value = riskLevel.trim().toLowerCase(Locale.ROOT);
        return value.contains("medium") || value.contains("中");
    }

    private boolean isHighSensitivity(String sensitivityLevel) {
        if (sensitivityLevel == null) return false;
        String value = sensitivityLevel.trim().toLowerCase(Locale.ROOT);
        return value.contains("high") || value.contains("critical") || value.contains("高");
    }
}