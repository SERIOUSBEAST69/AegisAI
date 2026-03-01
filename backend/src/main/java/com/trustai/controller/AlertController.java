package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.AlertRecord;
import com.trustai.service.AlertRecordService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AlertRecordService alertService;

    @GetMapping("/list")
    public R<List<AlertRecord>> list(@RequestParam(required = false) String status) {
        QueryWrapper<AlertRecord> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        return R.ok(alertService.list(qw));
    }

    @PostMapping("/create")
    public R<?> create(@RequestBody AlertRecord record) {
        record.setStatus("open");
        record.setCreateTime(new Date());
        alertService.save(record);
        return R.ok(record);
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody AlertRecord record) {
        record.setUpdateTime(new Date());
        alertService.updateById(record);
        return R.okMsg("更新成功");
    }
}
