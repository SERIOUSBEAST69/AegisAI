package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.RiskEvent;
import com.trustai.service.RiskEventService;
import com.trustai.service.RiskPredictionService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/risk")
public class RiskForecastController {

    @Autowired
    private RiskEventService riskEventService;

    @Autowired
    private RiskPredictionService riskPredictionService;

    @GetMapping("/trend")
    public R<Map<String, Object>> trend(@RequestParam(defaultValue = "7") int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<RiskEvent> events = riskEventService.list(new QueryWrapper<RiskEvent>().ge("create_time", fromDate));

        Map<Integer, Long> perHour = events.stream()
                .filter(e -> e.getCreateTime() != null)
                .collect(Collectors.groupingBy(e -> e.getCreateTime()
                        .toInstant().atZone(ZoneId.systemDefault()).getHour(), Collectors.counting()));

        double avg = perHour.values().stream().mapToLong(Long::longValue).average().orElse(0);
        Map<String, Object> res = new HashMap<>();
        res.put("perHour", perHour);
        res.put("forecastNextHour", perHour.isEmpty() ? 0 : Math.max(1, Math.round(avg + Math.sqrt(avg))));
        return R.ok(res);
    }

    /**
     * LSTM 风险预测接口：返回未来 7 天的风险事件数预测序列。
     * 调用 Python 微服务中的 LSTM 模型，不可用时降级为移动平均。
     */
    @GetMapping("/forecast")
    public R<Map<String, Object>> forecast() {
        List<Double> forecastSeries = riskPredictionService.forecastNext7Days();
        Map<String, Object> res = new HashMap<>();
        res.put("forecast", forecastSeries);
        res.put("horizon", forecastSeries.size());
        return R.ok(res);
    }
}
