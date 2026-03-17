package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trustai.entity.SecurityDetectionRule;
import com.trustai.entity.SecurityEvent;
import com.trustai.entity.User;
import com.trustai.service.CurrentUserService;
import com.trustai.service.SecurityDetectionRuleService;
import com.trustai.service.SecurityEventService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 安全事件（威胁监控）API。
 *
 * <p>提供：
 * <ul>
 *   <li>GET  /api/security/events        — 分页查询安全事件（需登录）</li>
 *   <li>POST /api/security/block         — 阻拦某事件（需登录）</li>
 *   <li>POST /api/security/ignore        — 忽略某事件（需登录）</li>
 *   <li>POST /api/security/events/report — 上报事件（无需登录，供模拟程序使用）</li>
 *   <li>GET  /api/security/rules         — 查询检测规则（需登录）</li>
 *   <li>POST /api/security/rules         — 新增/更新检测规则（需登录）</li>
 *   <li>GET  /api/security/stats         — 事件统计摘要（需登录）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/security")
public class SecurityEventController {

    private static final Set<String> VALID_STATUSES = Set.of("pending", "blocked", "ignored", "reviewing");
    private static final Set<String> VALID_SEVERITIES = Set.of("critical", "high", "medium", "low");

    @Autowired
    private SecurityEventService securityEventService;

    @Autowired
    private SecurityDetectionRuleService ruleService;

    @Autowired
    private CurrentUserService currentUserService;

    // ── 事件列表（分页） ──────────────────────────────────────────────────────────

    /**
     * GET /api/security/events
     *
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数（默认 20）
     * @param status   状态过滤（pending/blocked/ignored/reviewing）
     * @param severity 严重程度过滤（critical/high/medium/low）
     * @param keyword  关键字（匹配 filePath / hostname / employeeId）
     */
    @GetMapping("/events")
    public R<Map<String, Object>> events(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String keyword) {

        currentUserService.requireAnyRole("ADMIN", "SECOPS", "EXECUTIVE");

        // 校验枚举参数，防止非法值注入查询
        if (status != null && !VALID_STATUSES.contains(status)) {
            return R.error(40000, "status 参数非法，合法值：" + VALID_STATUSES);
        }
        if (severity != null && !VALID_SEVERITIES.contains(severity)) {
            return R.error(40000, "severity 参数非法，合法值：" + VALID_SEVERITIES);
        }
        if (keyword != null && keyword.length() > 200) {
            return R.error(40000, "keyword 长度不得超过 200 字符");
        }

        User currentUser = currentUserService.requireCurrentUser();
        boolean employeeOnly = currentUserService.isEmployeeUser();

        QueryWrapper<SecurityEvent> qw = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq("status", status);
        }
        if (severity != null && !severity.isBlank()) {
            qw.eq("severity", severity);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("file_path", keyword)
                    .or().like("hostname", keyword)
                    .or().like("employee_id", keyword));
        }
        if (employeeOnly) {
            qw.eq("employee_id", currentUser.getUsername());
        }
        qw.orderByDesc("event_time");

        Page<SecurityEvent> result = securityEventService.page(new Page<>(page, pageSize), qw);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("pages", result.getPages());
        data.put("current", result.getCurrent());
        data.put("list", result.getRecords());
        return R.ok(data);
    }

    // ── 阻拦事件 ────────────────────────────────────────────────────────────────

    /**
     * POST /api/security/block
     *
     * <p>将指定事件状态改为 "blocked"。
     */
    @PostMapping("/block")
    public R<?> block(@RequestBody IdReq req) {
        currentUserService.requireAnyRole("ADMIN", "SECOPS");
        SecurityEvent event = securityEventService.getById(req.getId());
        if (event == null) {
            return R.error(40400, "事件不存在");
        }
        event.setStatus("blocked");
        event.setOperatorId(currentUserService.requireCurrentUser().getId());
        event.setUpdateTime(new Date());
        securityEventService.updateById(event);
        return R.okMsg("已阻拦");
    }

    // ── 忽略事件 ────────────────────────────────────────────────────────────────

    /**
     * POST /api/security/ignore
     *
     * <p>将指定事件状态改为 "ignored"。
     */
    @PostMapping("/ignore")
    public R<?> ignore(@RequestBody IdReq req) {
        currentUserService.requireAnyRole("ADMIN", "SECOPS");
        SecurityEvent event = securityEventService.getById(req.getId());
        if (event == null) {
            return R.error(40400, "事件不存在");
        }
        event.setStatus("ignored");
        event.setOperatorId(currentUserService.requireCurrentUser().getId());
        event.setUpdateTime(new Date());
        securityEventService.updateById(event);
        return R.okMsg("已忽略");
    }

    // ── 上报事件（供模拟程序使用，无需登录） ──────────────────────────────────

    /**
     * POST /api/security/events/report
     *
     * <p>模拟程序通过此接口上报窃取事件。不需要登录 token，
     * 但需要请求体中包含合法的事件字段。
     */
    @PostMapping("/events/report")
    public R<?> report(@RequestBody SecurityEvent event) {
        if (event.getEventTime() == null) {
            event.setEventTime(new Date());
        }
        if (event.getStatus() == null || event.getStatus().isBlank()) {
            event.setStatus("pending");
        }
        if (event.getSource() == null || event.getSource().isBlank()) {
            event.setSource("agent");
        }
        event.setId(null);
        event.setCreateTime(new Date());
        event.setUpdateTime(new Date());
        securityEventService.save(event);
        Long newId = event.getId();
        return R.ok(newId != null ? Map.of("id", newId) : Map.of());
    }

    // ── 检测规则 ────────────────────────────────────────────────────────────────

    /** GET /api/security/rules — 查询所有检测规则 */
    @GetMapping("/rules")
    public R<List<SecurityDetectionRule>> rules() {
        currentUserService.requireAnyRole("ADMIN", "SECOPS");
        return R.ok(ruleService.list(new QueryWrapper<SecurityDetectionRule>().orderByAsc("id")));
    }

    /** POST /api/security/rules — 新增或更新检测规则 */
    @PostMapping("/rules")
    public R<?> saveRule(@RequestBody SecurityDetectionRule rule) {
        currentUserService.requireAnyRole("ADMIN", "SECOPS");
        if (rule.getCreateTime() == null) rule.setCreateTime(new Date());
        rule.setUpdateTime(new Date());
        ruleService.saveOrUpdate(rule);
        return R.okMsg("保存成功");
    }

    /** DELETE /api/security/rules/{id} — 删除检测规则 */
    @DeleteMapping("/rules/{id}")
    public R<?> deleteRule(@PathVariable Long id) {
        currentUserService.requireAnyRole("ADMIN", "SECOPS");
        ruleService.removeById(id);
        return R.okMsg("删除成功");
    }

    // ── 统计摘要 ────────────────────────────────────────────────────────────────

    /** GET /api/security/stats — 事件统计摘要 */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "EXECUTIVE");
        User currentUser = currentUserService.requireCurrentUser();
        boolean employeeOnly = currentUserService.isEmployeeUser();

        long total = securityEventService.count(scopedQuery(currentUser, employeeOnly));
        long pending = securityEventService.count(scopedQuery(currentUser, employeeOnly).eq("status", "pending"));
        long blocked = securityEventService.count(scopedQuery(currentUser, employeeOnly).eq("status", "blocked"));
        long critical = securityEventService.count(scopedQuery(currentUser, employeeOnly).eq("severity", "critical"));
        long high = securityEventService.count(scopedQuery(currentUser, employeeOnly).eq("severity", "high"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("pending", pending);
        data.put("blocked", blocked);
        data.put("critical", critical);
        data.put("high", high);
        return R.ok(data);
    }

    private QueryWrapper<SecurityEvent> scopedQuery(User user, boolean employeeOnly) {
        QueryWrapper<SecurityEvent> query = new QueryWrapper<>();
        if (employeeOnly && user != null) {
            query.eq("employee_id", user.getUsername());
        }
        return query;
    }

    // ── 内部类 ────────────────────────────────────────────────────────────────

    public static class IdReq {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}
