package com.trustai.controller;

import com.trustai.client.AiInferenceClient;
import com.trustai.service.CurrentUserService;
import com.trustai.utils.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工 AI 行为异常检测 API。
 *
 * <p>代理至 Python 推理服务（孤立森林模型），
 * 对员工 AI 使用行为进行实时异常评分，并将异常事件写入日志。
 *
 * <p>工作流程：
 * <ol>
 *   <li>前端/客户端上报一条 AI 使用行为记录（employee_id, department, ai_service, ...）</li>
 *   <li>Python 服务使用 IsolationForest 计算异常分数</li>
 *   <li>若检测为异常，将事件写入 SQLite 日志并输出 WARNING 日志</li>
 *   <li>本接口返回 is_anomaly、risk_level 和人类可读描述</li>
 * </ol>
 *
 * <p>需先运行训练脚本：
 * <pre>
 *   cd python-service
 *   python gen_behavior_data.py
 *   python train_anomaly.py
 * </pre>
 */
@RestController
@RequestMapping("/api/anomaly")
public class AnomalyController {

    private static final Logger log = LoggerFactory.getLogger(AnomalyController.class);

    @Autowired
    private AiInferenceClient aiInferenceClient;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * 检测单条员工 AI 行为记录是否异常。
     *
     * <p>POST /api/anomaly/check
     *
     * <p>请求体示例：
     * <pre>
     * {
     *   "employee_id":        "EMP_R0001",
     *   "department":         "研发",
     *   "ai_service":         "ChatGPT",
     *   "hour_of_day":        2,
     *   "day_of_week":        1,
     *   "message_length":     3500,
     *   "topic_code":         0,
     *   "session_duration_min": 90,
     *   "is_new_service":     0
     * }
     * </pre>
     */
    @PostMapping("/check")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','SECOPS','DATA_ADMIN','AI_BUILDER','BUSINESS_OWNER','EMPLOYEE')")
    public R<Map<String, Object>> check(@RequestBody(required = false) Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return R.error("请求体不能为空");
        }
        if (!currentUserService.hasAnyRole("ADMIN", "SECOPS")) {
            payload.put("employee_id", currentUserService.requireCurrentUser().getUsername());
        }
        try {
            Map<String, Object> result = aiInferenceClient.anomalyCheck(payload);
            return R.ok(result);
        } catch (Exception e) {
            log.error("[Anomaly] 异常检测失败: {}", e.getMessage());
            return R.error("异常检测服务暂不可用，请先运行 train_anomaly.py 训练模型");
        }
    }

    /**
     * 查询异常事件日志（最近 50 条）。
     *
     * <p>GET /api/anomaly/events
     */
    @GetMapping("/events")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','EXECUTIVE','SECOPS','DATA_ADMIN','AI_BUILDER','BUSINESS_OWNER','EMPLOYEE')")
    public R<Map<String, Object>> events() {
        try {
            Map<String, Object> result = aiInferenceClient.anomalyEvents();
            if (currentUserService.hasRole("EXECUTIVE")) {
                return R.ok(summaryForExecutive(result));
            }
            if (!currentUserService.hasAnyRole("ADMIN", "SECOPS")) {
                result = filterEventsForEmployee(result, currentUserService.requireCurrentUser().getUsername());
            }
            return R.ok(result);
        } catch (Exception e) {
            log.error("[Anomaly] 查询事件日志失败: {}", e.getMessage());
            return R.error("查询异常事件失败");
        }
    }

    /**
     * 获取异常检测模型当前状态（是否已训练、元信息等）。
     *
     * <p>GET /api/anomaly/status
     */
    @GetMapping("/status")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','EXECUTIVE','SECOPS','DATA_ADMIN','AI_BUILDER','BUSINESS_OWNER','EMPLOYEE')")
    public R<Map<String, Object>> status() {
        try {
            Map<String, Object> result = aiInferenceClient.anomalyStatus();
            return R.ok(result);
        } catch (Exception e) {
            log.error("[Anomaly] 查询模型状态失败: {}", e.getMessage());
            return R.error("模型状态查询失败，请检查 Python 推理服务是否启动");
        }
    }

    private Map<String, Object> filterEventsForEmployee(Map<String, Object> result, String employeeId) {
        if (result == null || !result.containsKey("events")) {
            return result;
        }
        Object rawEvents = result.get("events");
        if (!(rawEvents instanceof List<?> eventList)) {
            return result;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Object item : eventList) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> event = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null) {
                    event.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            Object eventEmployee = event.get("employee_id");
            if (eventEmployee != null && employeeId.equalsIgnoreCase(String.valueOf(eventEmployee))) {
                filtered.add(event);
            }
        }
        Map<String, Object> safe = new LinkedHashMap<>(result);
        safe.put("events", filtered);
        safe.put("count", filtered.size());
        return safe;
    }

    private Map<String, Object> summaryForExecutive(Map<String, Object> result) {
        Map<String, Object> source = result == null ? Map.of() : result;
        Object rawEvents = source.get("events");
        int total = 0;
        int anomaly = 0;
        if (rawEvents instanceof List<?> eventList) {
            total = eventList.size();
            for (Object item : eventList) {
                if (item instanceof Map<?, ?> map) {
                    Object flag = map.get("is_anomaly");
                    if (Boolean.TRUE.equals(flag)
                            || "true".equalsIgnoreCase(String.valueOf(flag))
                            || "1".equals(String.valueOf(flag))) {
                        anomaly++;
                    }
                }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("summaryOnly", true);
        summary.put("total", total);
        summary.put("anomalyCount", anomaly);
        summary.put("normalCount", Math.max(0, total - anomaly));
        summary.put("anomalyRate", total == 0 ? 0 : (double) anomaly / (double) total);
        summary.put("events", List.of());
        summary.put("count", 0);
        return summary;
    }
}
