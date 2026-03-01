package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.SubjectRequest;
import com.trustai.service.SubjectRequestService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/subject-request")
public class SubjectRequestController {

    @Autowired
    private SubjectRequestService subjectRequestService;

    @GetMapping("/list")
    public R<List<SubjectRequest>> list(@RequestParam(required = false) String status) {
        QueryWrapper<SubjectRequest> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        return R.ok(subjectRequestService.list(qw));
    }

    @PostMapping("/create")
    public R<?> create(@RequestBody SubjectRequest req) {
        req.setStatus("pending");
        req.setCreateTime(new Date());
        subjectRequestService.save(req);
        return R.ok(req);
    }

    @PostMapping("/process")
    public R<?> process(@RequestBody SubjectRequest req) {
        req.setUpdateTime(new Date());
        subjectRequestService.updateById(req);
        return R.okMsg("处理完成");
    }
}
