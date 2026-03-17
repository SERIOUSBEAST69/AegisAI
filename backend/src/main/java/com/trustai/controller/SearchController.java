package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.document.AssetDocument;
import com.trustai.document.ModelDocument;
import com.trustai.entity.*;
import com.trustai.repository.AssetEsRepository;
import com.trustai.repository.ModelEsRepository;
import com.trustai.service.*;
import com.trustai.utils.R;
import com.trustai.utils.AesEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.StreamSupport;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired private DataAssetService dataAssetService;
    @Autowired private AiModelService aiModelService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private ApprovalRequestService approvalRequestService;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private AesEncryptor aesEncryptor;
    @Autowired private AssetEsRepository assetEsRepository;
    @Autowired private ModelEsRepository modelEsRepository;

    @GetMapping("/global")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','EXECUTIVE','SECOPS','DATA_ADMIN','AI_BUILDER','BUSINESS_OWNER','EMPLOYEE')")
    public R<Map<String, Object>> search(@RequestParam String keyword) {
        User currentUser = currentUserService.requireCurrentUser();
        String roleCode = currentUserService.currentRoleCode();
        boolean employeeView = "EMPLOYEE".equalsIgnoreCase(roleCode);
        Map<String, Object> res = new HashMap<>();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<AssetDocument> assets;
        if (employeeView) {
            assets = dataAssetService.list(new QueryWrapper<DataAsset>()
                    .eq("owner_id", currentUser.getId()))
                .stream()
                .filter(asset -> containsIgnoreCase(asset.getName(), normalizedKeyword)
                    || containsIgnoreCase(asset.getDescription(), normalizedKeyword)
                    || containsIgnoreCase(asset.getType(), normalizedKeyword)
                    || containsIgnoreCase(asset.getSensitivityLevel(), normalizedKeyword)
                    || containsIgnoreCase(asset.getLocation(), normalizedKeyword))
                .map(asset -> {
                    AssetDocument doc = new AssetDocument();
                    doc.setAssetId(asset.getId());
                    doc.setName(asset.getName());
                    doc.setDescription(asset.getDescription());
                    doc.setType(asset.getType());
                    doc.setSensitivityLevel(asset.getSensitivityLevel());
                    doc.setLocation(asset.getLocation());
                    return doc;
                })
                .collect(Collectors.toList());
        } else {
            assets = StreamSupport.stream(assetEsRepository.findAll().spliterator(), false)
                .filter(doc -> containsIgnoreCase(doc.getName(), normalizedKeyword)
                    || containsIgnoreCase(doc.getDescription(), normalizedKeyword)
                    || containsIgnoreCase(doc.getType(), normalizedKeyword)
                    || containsIgnoreCase(doc.getSensitivityLevel(), normalizedKeyword)
                    || containsIgnoreCase(doc.getLocation(), normalizedKeyword))
                .collect(Collectors.toList());
        }

        List<ModelDocument> modelDocs = employeeView
            ? List.of()
            : StreamSupport.stream(modelEsRepository.findAll().spliterator(), false)
                .filter(doc -> containsIgnoreCase(doc.getModelName(), normalizedKeyword)
                    || containsIgnoreCase(doc.getModelCode(), normalizedKeyword)
                    || containsIgnoreCase(doc.getProvider(), normalizedKeyword)
                    || containsIgnoreCase(doc.getModelType(), normalizedKeyword)
                    || containsIgnoreCase(doc.getRiskLevel(), normalizedKeyword)
                    || containsIgnoreCase(doc.getDescription(), normalizedKeyword))
                .collect(Collectors.toList());

        res.put("assets", assets);
        List<AiModel> models = modelDocs.stream().map(doc -> {
            AiModel m = new AiModel();
            m.setId(doc.getModelId());
            m.setModelName(doc.getModelName());
            m.setModelCode(doc.getModelCode());
            m.setProvider(doc.getProvider());
            m.setModelType(doc.getModelType());
            m.setRiskLevel(doc.getRiskLevel());
            m.setStatus(doc.getStatus());
            m.setDescription(doc.getDescription());
            return m;
        }).collect(Collectors.toList());
        models.forEach(m -> {
            m.setName(m.getModelName());
        });
        res.put("models", models);
        res.put("auditLogs", auditLogService.search(employeeView ? currentUser.getId() : null, keyword, null, null));

        QueryWrapper<ApprovalRequest> qwApproval = new QueryWrapper<>();
        qwApproval.like("reason", keyword).or().like("status", keyword);
        if (employeeView) {
            qwApproval.eq("applicant_id", currentUser.getId());
        }
        res.put("approvals", approvalRequestService.list(qwApproval));
        return R.ok(res);
    }

    private boolean containsIgnoreCase(String source, String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.isEmpty()) {
            return true;
        }
        return source != null && source.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }
}
