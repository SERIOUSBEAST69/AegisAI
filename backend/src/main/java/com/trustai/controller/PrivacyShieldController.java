package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trustai.entity.PrivacyEvent;
import com.trustai.entity.User;
import com.trustai.service.CurrentUserService;
import com.trustai.service.PrivacyEventService;
import com.trustai.service.PrivacyShieldConfigService;
import com.trustai.utils.R;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
public class PrivacyShieldController {

    private static final Pattern ID_CARD = Pattern.compile("[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern BANK_CARD = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");
    private static final Pattern COMPANY_CODE = Pattern.compile("(?<![A-Z0-9])[0-9A-Z]{18}(?![A-Z0-9])");

    private final PrivacyEventService privacyEventService;
    private final PrivacyShieldConfigService privacyShieldConfigService;
    private final CurrentUserService currentUserService;

    @PostMapping("/events")
    public R<Map<String, Object>> reportEvent(@RequestBody PrivacyEventReportReq req) {
        if (req == null) {
            return R.error(40000, "请求体不能为空");
        }

        String content = StringUtils.hasText(req.getContent()) ? req.getContent() : "";
        String contentMasked = maskSensitive(content);
        List<String> matched = StringUtils.hasText(req.getMatchedTypes())
                ? List.of(req.getMatchedTypes().split(","))
                : detectMatchedTypes(content);

        PrivacyEvent event = new PrivacyEvent();
        event.setUserId(resolveUserId(req.getUserId()));
        event.setEventType(StringUtils.hasText(req.getEventType()) ? req.getEventType() : "SENSITIVE_TEXT");
        event.setContentMasked(contentMasked);
        event.setSource(StringUtils.hasText(req.getSource()) ? req.getSource() : "extension");
        event.setAction(StringUtils.hasText(req.getAction()) ? req.getAction() : "detect");
        event.setDeviceId(StringUtils.hasText(req.getDeviceId()) ? req.getDeviceId() : null);
        event.setHostname(StringUtils.hasText(req.getHostname()) ? req.getHostname() : null);
        event.setWindowTitle(StringUtils.hasText(req.getWindowTitle()) ? req.getWindowTitle() : null);
        event.setMatchedTypes(String.join(",", matched));
        event.setEventTime(resolveTime(req.getTimestamp()));
        event.setCreateTime(new Date());
        event.setUpdateTime(new Date());

        privacyEventService.save(event);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", event.getId());
        data.put("matchedTypes", matched);
        return R.ok(data);
    }

    @GetMapping("/events")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','EXECUTIVE','SECOPS','DATA_ADMIN','AI_BUILDER','BUSINESS_OWNER','EMPLOYEE')")
    public R<Map<String, Object>> listEvents(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        User currentUser = currentUserService.requireCurrentUser();
        boolean adminOrSecops = currentUserService.hasAnyRole("ADMIN", "SECOPS");
        boolean executive = currentUserService.hasRole("EXECUTIVE");
        String scopedUserId;
        if (adminOrSecops) {
            scopedUserId = userId;
        } else if (executive) {
            scopedUserId = null;
        } else {
            scopedUserId = currentUser.getUsername();
        }

        Map<String, Object> summary = buildSummary(scopedUserId, eventType, source, action, startTime, endTime);
        if (executive) {
            summary.put("summaryOnly", true);
            summary.put("list", List.of());
            summary.put("total", summary.get("total"));
            summary.put("pages", 0);
            summary.put("current", 1);
            return R.ok(summary);
        }

        QueryWrapper<PrivacyEvent> query = buildQuery(scopedUserId, eventType, source, action, startTime, endTime)
                .orderByDesc("event_time");

        Page<PrivacyEvent> result = privacyEventService.page(new Page<>(Math.max(1, page), Math.max(1, pageSize)), query);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summaryOnly", false);
        data.put("total", result.getTotal());
        data.put("pages", result.getPages());
        data.put("current", result.getCurrent());
        data.put("list", result.getRecords());
        data.putAll(summary);
        return R.ok(data);
    }

    @GetMapping("/config")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','SECOPS')")
    public R<Map<String, Object>> getConfig() {
        return R.ok(privacyShieldConfigService.getOrCreateConfig());
    }

    @GetMapping("/config/public")
    public R<Map<String, Object>> getPublicConfig() {
        return R.ok(privacyShieldConfigService.getOrCreateConfig());
    }

    @PostMapping("/config")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','SECOPS')")
    public R<Map<String, Object>> updateConfig(@RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> updated = privacyShieldConfigService.updateConfig(payload == null ? Map.of() : payload);
        return R.ok(updated);
    }

    private QueryWrapper<PrivacyEvent> buildQuery(String userId, String eventType, String source, String action,
                                                  String startTime, String endTime) {
        QueryWrapper<PrivacyEvent> query = new QueryWrapper<>();
        if (StringUtils.hasText(userId)) {
            query.eq("user_id", userId);
        }
        if (StringUtils.hasText(eventType)) {
            query.eq("event_type", eventType);
        }
        if (StringUtils.hasText(source)) {
            query.eq("source", source);
        }
        if (StringUtils.hasText(action)) {
            query.eq("action", action);
        }
        Date start = parseFlexibleDate(startTime, false);
        Date end = parseFlexibleDate(endTime, true);
        if (start != null) {
            query.ge("event_time", start);
        }
        if (end != null) {
            query.le("event_time", end);
        }
        return query;
    }

    private Map<String, Object> buildSummary(String userId, String eventType, String source,
                                             String action, String startTime, String endTime) {
        Map<String, Object> summary = new LinkedHashMap<>();

        long total = privacyEventService.count(buildQuery(userId, eventType, source, action, startTime, endTime));

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        long today = privacyEventService.count(buildQuery(userId, eventType, source, action, startTime, endTime)
                .ge("event_time", Date.from(dayStart.atZone(ZoneId.systemDefault()).toInstant())));

        long extensionCount = privacyEventService.count(buildQuery(userId, eventType, "extension", action, startTime, endTime));
        long clipboardCount = privacyEventService.count(buildQuery(userId, eventType, "clipboard", action, startTime, endTime));
        long ignoreCount = privacyEventService.count(buildQuery(userId, eventType, source, "ignore", startTime, endTime));
        long desenseCount = privacyEventService.count(buildQuery(userId, eventType, source, "desensitize", startTime, endTime));

        summary.put("total", total);
        summary.put("today", today);
        summary.put("extensionCount", extensionCount);
        summary.put("clipboardCount", clipboardCount);
        summary.put("ignoreCount", ignoreCount);
        summary.put("desensitizeCount", desenseCount);
        return summary;
    }

    private String resolveUserId(String requestUserId) {
        if (StringUtils.hasText(requestUserId)) {
            return requestUserId;
        }
        try {
            return currentUserService.requireCurrentUser().getUsername();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private Date resolveTime(String timestamp) {
        Date parsed = parseFlexibleDate(timestamp, false);
        return parsed == null ? new Date() : parsed;
    }

    private Date parseFlexibleDate(String value, boolean endOfDayWhenDateOnly) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String input = value.trim();

        if (input.matches("^\\d{10,13}$")) {
            long epoch = Long.parseLong(input);
            if (input.length() == 10) {
                epoch = epoch * 1000;
            }
            return new Date(epoch);
        }

        try {
            return Date.from(Instant.parse(input));
        } catch (DateTimeParseException ignored) {
            // continue parse by formatter
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            LocalDate date = LocalDate.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime dt = endOfDayWhenDateOnly ? date.atTime(23, 59, 59) : date.atStartOfDay();
            return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            // continue
        }
        return null;
    }

    private List<String> detectMatchedTypes(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        List<String> matched = new ArrayList<>();
        if (ID_CARD.matcher(content).find()) {
            matched.add("id_card");
        }
        if (PHONE.matcher(content).find()) {
            matched.add("phone");
        }
        if (BANK_CARD.matcher(content).find()) {
            matched.add("bank_card");
        }
        if (COMPANY_CODE.matcher(content).find()) {
            matched.add("company_code");
        }
        return matched;
    }

    private String maskSensitive(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        String masked = content;
        masked = ID_CARD.matcher(masked).replaceAll(matchResult -> maskIdCard(matchResult.group()));
        masked = PHONE.matcher(masked).replaceAll(matchResult -> maskPhone(matchResult.group()));
        masked = BANK_CARD.matcher(masked).replaceAll(matchResult -> maskBankCard(matchResult.group()));
        masked = COMPANY_CODE.matcher(masked).replaceAll(matchResult -> maskCompanyCode(matchResult.group()));
        return masked;
    }

    private String maskIdCard(String value) {
        if (!StringUtils.hasText(value) || value.length() < 10) {
            return value;
        }
        return value.substring(0, 6) + "******" + value.substring(value.length() - 4);
    }

    private String maskPhone(String value) {
        if (!StringUtils.hasText(value) || value.length() != 11) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(7);
    }

    private String maskBankCard(String value) {
        if (!StringUtils.hasText(value) || value.length() < 10) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private String maskCompanyCode(String value) {
        if (!StringUtils.hasText(value) || value.length() < 8) {
            return value;
        }
        return value.substring(0, 4) + "******" + value.substring(value.length() - 4);
    }

    @Data
    public static class PrivacyEventReportReq {
        private String userId;
        private String eventType;
        private String content;
        private String source;
        private String timestamp;
        private String action;
        private String deviceId;
        private String hostname;
        private String windowTitle;
        private String matchedTypes;
    }
}
