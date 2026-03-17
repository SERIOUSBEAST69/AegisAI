package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.ModelCallStat;
import com.trustai.service.ModelCallStatService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

@RestController
@RequestMapping("/api/model-stat")
public class ModelStatController {

    @Autowired
    private ModelCallStatService statService;

    @GetMapping("/list")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','EXECUTIVE','AI_BUILDER','BUSINESS_OWNER')")
    public R<List<ModelCallStat>> list(@RequestParam(required = false) Long modelId,
                                       @RequestParam(required = false) Long userId) {
        QueryWrapper<ModelCallStat> qw = new QueryWrapper<>();
        if (modelId != null) qw.eq("model_id", modelId);
        if (userId != null) qw.eq("user_id", userId);
        return R.ok(statService.list(qw));
    }

    @GetMapping("/summary")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','EXECUTIVE','AI_BUILDER','BUSINESS_OWNER')")
    public R<Map<String, Object>> summary(@RequestParam(required = false) Long modelId,
                                          @RequestParam(required = false) Long userId) {
        QueryWrapper<ModelCallStat> qw = new QueryWrapper<>();
        if (modelId != null) qw.eq("model_id", modelId);
        if (userId != null) qw.eq("user_id", userId);
        List<ModelCallStat> stats = statService.list(qw);
        long calls = stats.stream().mapToLong(s -> s.getCallCount() == null ? 0 : s.getCallCount()).sum();
        long latency = stats.stream().mapToLong(s -> s.getTotalLatencyMs() == null ? 0 : s.getTotalLatencyMs()).sum();
        long cost = stats.stream().mapToLong(s -> s.getCostCents() == null ? 0 : s.getCostCents()).sum();
        Map<String, Object> map = new HashMap<>();
        map.put("callCount", calls);
        map.put("avgLatencyMs", calls == 0 ? 0 : latency / calls);
        map.put("costCents", cost);
        map.put("from", stats.stream().map(ModelCallStat::getDate).min(Date::compareTo).orElse(null));
        map.put("to", stats.stream().map(ModelCallStat::getDate).max(Date::compareTo).orElse(null));
        return R.ok(map);
    }
}
