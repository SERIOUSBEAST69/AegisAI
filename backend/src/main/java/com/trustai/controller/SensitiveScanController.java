package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.SensitiveScanTask;
import com.trustai.service.SensitiveScanTaskService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/sensitive-scan")
public class SensitiveScanController {

    @Autowired
    private SensitiveScanTaskService taskService;

    @PostMapping("/create")
    public R<SensitiveScanTask> create(@RequestBody SensitiveScanTask task) {
        task.setStatus("pending");
        task.setCreateTime(new Date());
        taskService.save(task);
        return R.ok(task);
    }

    @GetMapping("/list")
    public R<List<SensitiveScanTask>> list(@RequestParam(required = false) String status) {
        QueryWrapper<SensitiveScanTask> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        return R.ok(taskService.list(qw));
    }

    @PostMapping("/run")
    public R<SensitiveScanTask> run(@RequestBody IdReq req) {
        SensitiveScanTask task = taskService.getById(req.getId());
        if (task == null) return R.error(40000, "任务不存在");
        task.setStatus("done");
        task.setSensitiveRatio(new Random().nextDouble() * 50 + 10); // 10%-60% 随机占比
        task.setReportPath("/reports/task-" + task.getId() + ".pdf");
        task.setUpdateTime(new Date());
        taskService.updateById(task);
        return R.ok(task);
    }

    public static class IdReq { private Long id; public Long getId(){return id;} public void setId(Long id){this.id=id;} }
}
