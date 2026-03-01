package com.trustai.controller;

import com.trustai.service.*;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Autowired private DataAssetService dataAssetService;
    @Autowired private AiModelService aiModelService;
    @Autowired private UserService userService;
    @Autowired private RiskEventService riskEventService;

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Map<String, Object> map = new HashMap<>();
        map.put("dataAsset", dataAssetService.count());
        map.put("aiModel", aiModelService.count());
        map.put("user", userService.count());
        map.put("riskEvent", riskEventService.count());
        return R.ok(map);
    }
}
