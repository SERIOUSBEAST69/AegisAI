package com.trustai.controller;

import com.trustai.entity.DesensitizeRule;
import com.trustai.dto.DesenseRecommendationDto;
import com.trustai.service.RecommendationService;
import com.trustai.utils.R;
import com.trustai.service.DesensitizeRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/desense")
@Validated
public class DesenseController {

    @Autowired
    private DesensitizeRuleService ruleService;
    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/rules")
    public R<List<DesensitizeRule>> rules() {
        return R.ok(ruleService.list());
    }

    @PostMapping("/save")
    public R<?> save(@RequestBody DesensitizeRule rule) {
        if (rule.getId() == null) ruleService.save(rule); else ruleService.updateById(rule);
        return R.ok(rule);
    }

    @PostMapping("/recommend")
    public R<List<DesenseRecommendationDto>> recommend(@RequestBody RecommendReq req) {
        List<DesenseRecommendationDto> data = recommendationService.recommend(req.getDataCategory(), req.getUserRole(), req.getSensitivityLevel());
        return R.ok(data);
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        ruleService.removeById(req.getId());
        return R.okMsg("删除成功");
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

    public static class RecommendReq {
        private String dataCategory;
        private String userRole;
        private String sensitivityLevel;
        public String getDataCategory(){return dataCategory;}
        public void setDataCategory(String dataCategory){this.dataCategory=dataCategory;}
        public String getUserRole(){return userRole;}
        public void setUserRole(String userRole){this.userRole=userRole;}
        public String getSensitivityLevel(){return sensitivityLevel;}
        public void setSensitivityLevel(String sensitivityLevel){this.sensitivityLevel=sensitivityLevel;}
    }

    public static class IdReq {
        @NotNull private Long id;
        public Long getId(){return id;}
        public void setId(Long id){this.id=id;}
    }
}
