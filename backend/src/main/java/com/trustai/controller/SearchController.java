package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.*;
import com.trustai.service.*;
import com.trustai.utils.R;
import com.trustai.utils.AesEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired private DataAssetService dataAssetService;
    @Autowired private AiModelService aiModelService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private ApprovalRequestService approvalRequestService;
    @Autowired private AesEncryptor aesEncryptor;

    @GetMapping("/global")
    public R<Map<String, Object>> search(@RequestParam String keyword) {
        Map<String, Object> res = new HashMap<>();
        QueryWrapper<DataAsset> qwAsset = new QueryWrapper<>();
        qwAsset.like("name", keyword).or().like("description", keyword);
        QueryWrapper<AiModel> qwModel = new QueryWrapper<>();
        qwModel.like("model_name", keyword).or().like("description", keyword);
        QueryWrapper<AuditLog> qwLog = new QueryWrapper<>();
        qwLog.like("operation", keyword).or().like("input_overview", keyword).last("limit 50");
        QueryWrapper<ApprovalRequest> qwApproval = new QueryWrapper<>();
        qwApproval.like("reason", keyword).or().like("status", keyword);
        res.put("assets", dataAssetService.list(qwAsset));
        List<AiModel> models = aiModelService.list(qwModel);
        models.forEach(m -> m.setApiKey(aesEncryptor.mask(m.getApiKey())));
        res.put("models", models);
        res.put("auditLogs", auditLogService.list(qwLog));
        res.put("approvals", approvalRequestService.list(qwApproval));
        return R.ok(res);
    }
}
