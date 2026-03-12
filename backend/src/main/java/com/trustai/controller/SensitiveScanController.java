package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustai.dto.SensitiveScanReport;
import com.trustai.entity.SensitiveScanTask;
import com.trustai.service.SensitiveScanEngine;
import com.trustai.service.SensitiveScanTaskService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/sensitive-scan")
@Validated
public class SensitiveScanController {

    @Autowired
    private SensitiveScanTaskService taskService;
    @Autowired
    private SensitiveScanEngine scanEngine;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PostMapping("/create")
    public R<SensitiveScanTask> create(@RequestBody @Validated CreateReq req) {
        SensitiveScanTask task = new SensitiveScanTask();
        task.setAssetId(req.getAssetId());
        task.setSourceType(req.getSourceType());
        task.setSourcePath(req.getSourcePath());
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
    public R<SensitiveScanTask> run(@RequestBody @Validated IdReq req) {
        SensitiveScanTask task = taskService.getById(req.getId());
        if (task == null) return R.error(40000, "任务不存在");
        List<String> samples = task.getSourcePath() == null ? List.of("待扫描文本样例") : List.of(task.getSourcePath());
        SensitiveScanReport report = scanEngine.scan(samples);
        try {
            task.setReportData(MAPPER.writeValueAsString(report));
        } catch (Exception e) {
            task.setReportData(null);
        }
        task.setSensitiveRatio(report.getSummary() == null ? 0.0 : report.getSummary().getRatio());
        task.setStatus("done");
        task.setReportPath("/reports/task-" + task.getId() + ".json");
        task.setUpdateTime(new Date());
        taskService.updateById(task);
        return R.ok(task);
    }

    @GetMapping("/{id}/report")
    public R<?> report(@PathVariable Long id) {
        SensitiveScanTask task = taskService.getById(id);
        if (task == null) return R.error(40000, "任务不存在");
        if (task.getReportData() == null) return R.error(40000, "报告未生成");
        return R.ok(task.getReportData());
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        taskService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq { @NotNull private Long id; public Long getId(){return id;} public void setId(Long id){this.id=id;} }
    public static class CreateReq { @NotBlank private String sourceType; @NotBlank private String sourcePath; private Long assetId; public String getSourceType(){return sourceType;} public void setSourceType(String v){sourceType=v;} public String getSourcePath(){return sourcePath;} public void setSourcePath(String v){sourcePath=v;} public Long getAssetId(){return assetId;} public void setAssetId(Long assetId){this.assetId=assetId;} }
}
