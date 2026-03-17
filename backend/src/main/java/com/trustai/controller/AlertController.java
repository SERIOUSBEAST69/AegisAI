package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.AlertRecord;
import com.trustai.service.AlertRecordService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AlertRecordService alertService;

    private static final Set<String> ALLOWED_STATUS = new HashSet<>(Arrays.asList("open", "claimed", "resolved", "archived"));

    @GetMapping("/list")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','SECOPS')")
    public R<List<AlertRecord>> list(@RequestParam(required = false) String status) {
        QueryWrapper<AlertRecord> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        return R.ok(alertService.list(qw));
    }

    @PostMapping("/create")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','SECOPS')")
    public R<?> create(@RequestBody AlertRecord record) {
        record.setStatus("open");
        record.setCreateTime(new Date());
        alertService.save(record);
        return R.ok(record);
    }

    @PostMapping("/update")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','SECOPS')")
    public R<?> update(@RequestBody AlertRecord record) {
        if (record.getId() == null) return R.error(40000, "缺少ID");
        AlertRecord current = alertService.getById(record.getId());
        if (current == null) return R.error(40000, "告警不存在");
        if (record.getStatus() != null && !ALLOWED_STATUS.contains(record.getStatus())) {
            return R.error(40000, "不支持的状态");
        }
        if (record.getStatus() != null) current.setStatus(record.getStatus());
        if (record.getAssigneeId() != null) current.setAssigneeId(record.getAssigneeId());
        if (record.getResolution() != null) current.setResolution(record.getResolution());
        current.setUpdateTime(new Date());
        alertService.updateById(current);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    @PreAuthorize("@currentUserService.hasAnyRole('ADMIN','SECOPS')")
    public R<?> delete(@RequestBody IdReq req) {
        alertService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}
