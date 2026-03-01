package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.AuditLog;
import com.trustai.service.AuditLogService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {
    @Autowired private AuditLogService auditLogService;

    @PostMapping("/search")
    public R<List<AuditLog>> search(@RequestBody AuditLog query) {
        QueryWrapper<AuditLog> qw = new QueryWrapper<>();
        if (query.getUserId() != null) qw.eq("user_id", query.getUserId());
        if (query.getAssetId() != null) qw.eq("asset_id", query.getAssetId());
        List<AuditLog> list = auditLogService.list(qw);
        return R.ok(list);
    }
}
