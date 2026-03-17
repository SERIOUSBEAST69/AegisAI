package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.ClientReport;
import com.trustai.entity.ClientScanQueue;
import com.trustai.service.ClientReportService;
import com.trustai.service.ClientScanQueueService;
import com.trustai.service.CurrentUserService;
import com.trustai.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 轻量级客户端 API – 负责接收客户端心跳/扫描报告，并对工作台提供影子AI治理视图。
 *
 * <h3>客户端集成流程</h3>
 * <ol>
 *   <li>客户端启动时调用 {@code POST /api/client/register} 完成注册，获取 clientId 确认。</li>
 *   <li>每次扫描完成后调用 {@code POST /api/client/report} 上报发现结果。</li>
 *   <li>管理员通过 {@code GET /api/client/list} 查看所有在线客户端及其发现的影子AI。</li>
 *   <li>{@code GET /api/client/stats} 为工作台首页提供摘要统计数据。</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientReportController {

    private final ClientReportService clientReportService;
    private final ClientScanQueueService clientScanQueueService;
    private final CurrentUserService currentUserService;

    // ── 客户端注册（幂等） ──────────────────────────────────────────────────────

    /**
     * 客户端首次启动或重新连接时调用。
     * 若该 clientId 已有历史报告则直接返回确认，否则记录初始化记录。
     */
    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody RegisterReq req) {
        if (req.getClientId() == null || req.getClientId().isBlank()) {
            return R.error(40000, "clientId 不能为空");
        }

        long existing = clientReportService.count(
                new QueryWrapper<ClientReport>().eq("client_id", req.getClientId())
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clientId", req.getClientId());
        result.put("registered", existing > 0);
        result.put("serverTime", LocalDateTime.now().toString());
        return R.ok(result);
    }

    // ── 上报扫描结果 ────────────────────────────────────────────────────────────

    /**
     * 客户端扫描完成后调用，将本次发现的影子AI服务列表上报给服务端。
     */
    @PostMapping("/report")
    public R<Map<String, Object>> report(@RequestBody ClientReport report) {
        if (report.getClientId() == null || report.getClientId().isBlank()) {
            return R.error(40000, "clientId 不能为空");
        }

        report.setId(null);
        if (report.getScanTime() == null) {
            report.setScanTime(LocalDateTime.now());
        }
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());

    // 计算综合风险等级（优先考虑服务的风险级别，其次考虑数量）
        report.setRiskLevel(calcRiskLevel(report.getShadowAiCount(), report.getDiscoveredServices()));

        clientReportService.save(report);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", report.getId());
        result.put("accepted", true);
        result.put("riskLevel", report.getRiskLevel());
        return R.ok(result);
    }

    // ── 管理端：查询客户端列表及最新报告 ──────────────────────────────────────

    /**
     * 返回所有客户端的最新一条扫描报告（按 client_id 去重，取最新）。
     */
    @GetMapping("/list")
    public R<List<ClientReport>> list() {
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN", "BUSINESS_OWNER");
        // 取全部报告，按 clientId 分组，每组保留最新一条
        List<ClientReport> all = clientReportService.list(
                new QueryWrapper<ClientReport>().orderByDesc("scan_time")
        );

        Map<String, ClientReport> latestByClient = new LinkedHashMap<>();
        for (ClientReport r : all) {
            latestByClient.putIfAbsent(r.getClientId(), r);
        }

        return R.ok(new ArrayList<>(latestByClient.values()));
    }

    /**
     * 查询指定客户端的历史报告（最近 50 条）。
     */
    @GetMapping("/history")
    public R<List<ClientReport>> history(@RequestParam String clientId) {
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN", "BUSINESS_OWNER");
        List<ClientReport> records = clientReportService.list(
                new QueryWrapper<ClientReport>()
                        .eq("client_id", clientId)
                        .orderByDesc("scan_time")
                        .last("LIMIT 50")
        );
        return R.ok(records);
    }

    // ── 工作台摘要统计 ──────────────────────────────────────────────────────────

    /**
     * 返回影子AI治理摘要，供工作台首页和 ShadowAiDiscovery 视图使用。
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN", "BUSINESS_OWNER");
        List<ClientReport> all = clientReportService.list(
                new QueryWrapper<ClientReport>().orderByDesc("scan_time")
        );

        // 每个客户端取最新报告
        Map<String, ClientReport> latestByClient = new LinkedHashMap<>();
        for (ClientReport r : all) {
            latestByClient.putIfAbsent(r.getClientId(), r);
        }

        Collection<ClientReport> latest = latestByClient.values();
        long totalClients = latest.size();
        long highRiskClients = latest.stream()
                .filter(r -> "high".equals(r.getRiskLevel()))
                .count();
        long totalShadowAi = latest.stream()
                .mapToLong(r -> r.getShadowAiCount() == null ? 0L : r.getShadowAiCount())
                .sum();

        // 近 7 天新增报告数
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recentReports = all.stream()
                .filter(r -> r.getScanTime() != null && r.getScanTime().isAfter(sevenDaysAgo))
                .count();

        // 风险等级分布
        Map<String, Long> riskDistribution = latest.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRiskLevel() == null ? "none" : r.getRiskLevel(),
                        Collectors.counting()
                ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalClients", totalClients);
        result.put("highRiskClients", highRiskClients);
        result.put("totalShadowAi", totalShadowAi);
        result.put("recentReports", recentReports);
        result.put("riskDistribution", riskDistribution);
        return R.ok(result);
    }

    // ── 云端扫描队列 ─────────────────────────────────────────────────────────────

    /**
     * 查询云端扫描队列（最新 50 条，按下载时间倒序）。
     * 当用户通过Web界面下载客户端并开启本地扫描时，相应记录将出现在此队列中。
     */
    @GetMapping("/queue")
    public R<List<ClientScanQueue>> queue() {
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN", "BUSINESS_OWNER");
        List<ClientScanQueue> items = clientScanQueueService.list(
                new QueryWrapper<ClientScanQueue>()
                        .orderByDesc("download_time")
                        .last("LIMIT 50")
        );
        return R.ok(items);
    }

    /**
     * 将一次客户端下载事件加入云端扫描队列。
     * 前端在用户点击下载按钮且本地扫描已开启时调用此接口。
     *
     * @param req 平台信息及发起下载的设备信息
     */
    @PostMapping("/queue")
    public R<ClientScanQueue> enqueue(@RequestBody QueueReq req, HttpServletRequest servletReq) {
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN", "BUSINESS_OWNER");
        ClientScanQueue entry = new ClientScanQueue();
        entry.setPlatform(req.getPlatform() != null ? req.getPlatform() : "unknown");
        entry.setHostname(req.getHostname());
        entry.setOsUsername(req.getOsUsername());
        entry.setUserAgent(servletReq.getHeader("User-Agent"));
        entry.setStatus("queued");
        entry.setDownloadTime(LocalDateTime.now());
        entry.setCreateTime(LocalDateTime.now());
        entry.setUpdateTime(LocalDateTime.now());
        clientScanQueueService.save(entry);
        return R.ok(entry);
    }

    /**
     * 更新队列中某条记录的扫描状态（由安装后的客户端或调度任务调用）。
     */
    @PostMapping("/queue/{id}/status")
    public R<?> updateQueueStatus(@PathVariable Long id, @RequestBody QueueStatusReq req) {
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN", "BUSINESS_OWNER");
        ClientScanQueue entry = clientScanQueueService.getById(id);
        if (entry == null) return R.error(40000, "队列记录不存在");
        entry.setStatus(req.getStatus());
        if (req.getScanResult() != null) entry.setScanResult(req.getScanResult());
        entry.setUpdateTime(LocalDateTime.now());
        clientScanQueueService.updateById(entry);
        return R.okMsg("状态已更新");
    }

    // ── 内部工具 ────────────────────────────────────────────────────────────────

    /**
     * 计算综合风险等级。
     * 优先根据服务的个人 riskLevel 字段判断（单个 high 服务即为 high），
     * 再按发现数量兜底判断。
     */
    private String calcRiskLevel(Integer count, String discoveredServicesJson) {
        if (count == null || count == 0) return "none";

        // 尝试从 JSON 中提取最高风险级别
        if (discoveredServicesJson != null && !discoveredServicesJson.isBlank()) {
            if (discoveredServicesJson.contains("\"riskLevel\":\"high\"")) return "high";
            if (discoveredServicesJson.contains("\"riskLevel\":\"medium\"")) return "medium";
        }

        // 兜底：按数量判断
        if (count >= 5) return "high";
        if (count >= 2) return "medium";
        return "low";
    }

    // ── DTO ────────────────────────────────────────────────────────────────────

    public static class RegisterReq {
        private String clientId;
        private String hostname;
        private String osUsername;
        private String osType;
        private String clientVersion;

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        public String getOsUsername() { return osUsername; }
        public void setOsUsername(String osUsername) { this.osUsername = osUsername; }
        public String getOsType() { return osType; }
        public void setOsType(String osType) { this.osType = osType; }
        public String getClientVersion() { return clientVersion; }
        public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
    }

    public static class QueueReq {
        private String platform;
        private String hostname;
        private String osUsername;

        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        public String getOsUsername() { return osUsername; }
        public void setOsUsername(String osUsername) { this.osUsername = osUsername; }
    }

    public static class QueueStatusReq {
        private String status;
        private String scanResult;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getScanResult() { return scanResult; }
        public void setScanResult(String scanResult) { this.scanResult = scanResult; }
    }
}
