package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.AuditLog;
import com.trustai.service.AuditLogService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-report")
public class AuditReportController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/compare")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','SECOPS','EXECUTIVE')")
    public R<Map<String, Object>> compare(@RequestParam String from, @RequestParam String to) {
        QueryWrapper<AuditLog> base = new QueryWrapper<>();
        base.between("operation_time", from + " 00:00:00", to + " 23:59:59");
        List<AuditLog> logs = auditLogService.list(base);
        long success = logs.stream().filter(l -> "成功".equals(l.getResult()) || "success".equalsIgnoreCase(l.getResult())).count();
        long fail = logs.size() - success;
        Map<String, Object> map = new HashMap<>();
        map.put("total", logs.size());
        map.put("success", success);
        map.put("fail", fail);
        return R.ok(map);
    }

    @GetMapping("/generate")
    @PreAuthorize("@currentUserService.hasRole('ADMIN')")
    public R<Map<String, String>> generate(@RequestParam(required = false) String range) {
        // 简化：返回报告下载占位链接
        Map<String, String> map = new HashMap<>();
        map.put("title", "合规审计报告" + (range == null ? "" : ("-" + range)));
        map.put("downloadUrl", "/reports/audit-" + System.currentTimeMillis() + ".pdf");
        return R.ok(map);
    }
}
