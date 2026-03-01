package com.trustai.controller;

import com.trustai.entity.DesensitizeRule;
import com.trustai.utils.R;
import com.trustai.service.DesensitizeRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/desense")
public class DesenseController {

    @Autowired
    private DesensitizeRuleService ruleService;

    @GetMapping("/rules")
    public R<List<DesensitizeRule>> rules() {
        return R.ok(ruleService.list());
    }

    @PostMapping("/save")
    public R<?> save(@RequestBody DesensitizeRule rule) {
        if (rule.getId() == null) ruleService.save(rule); else ruleService.updateById(rule);
        return R.ok(rule);
    }

    @PostMapping("/preview")
    public R<Map<String, String>> preview(@RequestBody PreviewReq req) {
        String masked = applyMask(req.getSample(), req.getMask());
        Map<String, String> map = new HashMap<>();
        map.put("raw", req.getSample());
        map.put("masked", masked);
        return R.ok(map);
    }

    private String applyMask(String sample, String mask) {
        if (sample == null) return "";
        if (mask == null || mask.isEmpty()) return sample;
        // 简单脱敏：保留前2后2，其他用掩码字符
        int keep = 2;
        if (sample.length() <= keep * 2) return mask.repeat(sample.length());
        StringBuilder sb = new StringBuilder();
        sb.append(sample, 0, keep);
        for (int i = keep; i < sample.length() - keep; i++) sb.append(mask.charAt(0));
        sb.append(sample, sample.length() - keep, sample.length());
        return sb.toString();
    }

    public static class PreviewReq {
        private String sample; private String mask;
        public String getSample(){return sample;} public void setSample(String s){this.sample=s;}
        public String getMask(){return mask;} public void setMask(String m){this.mask=m;}
    }
}
