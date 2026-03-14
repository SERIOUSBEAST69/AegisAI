package com.trustai.controller;

import com.trustai.client.AiInferenceClient;
import com.trustai.utils.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public R<Map<String, Object>> check(@RequestBody Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return R.error("请求体不能为空");
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
    public R<Map<String, Object>> events() {
        try {
            Map<String, Object> result = aiInferenceClient.anomalyEvents();
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
    public R<Map<String, Object>> status() {
        try {
            Map<String, Object> result = aiInferenceClient.anomalyStatus();
            return R.ok(result);
        } catch (Exception e) {
            log.error("[Anomaly] 查询模型状态失败: {}", e.getMessage());
            return R.error("模型状态查询失败，请检查 Python 推理服务是否启动");
        }
    }
}
