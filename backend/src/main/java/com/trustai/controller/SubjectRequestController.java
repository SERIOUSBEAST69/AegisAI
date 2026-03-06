package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.SubjectRequest;
import com.trustai.service.SubjectRequestService;
import com.trustai.utils.R;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/subject-request")
@Validated
public class SubjectRequestController {

    @Autowired
    private SubjectRequestService subjectRequestService;

    private static final Set<String> ALLOWED_STATUS = new HashSet<>(Arrays.asList("pending", "processing", "done", "rejected"));
    private static final Set<String> FINAL_STATUS = new HashSet<>(Arrays.asList("done", "rejected"));

    @GetMapping("/list")
    public R<List<SubjectRequest>> list(@RequestParam(required = false) String status) {
        QueryWrapper<SubjectRequest> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        return R.ok(subjectRequestService.list(qw));
    }

    @PostMapping("/create")
    public R<?> create(@RequestBody @Validated ApplyReq req) {
        SubjectRequest entity = new SubjectRequest();
        entity.setUserId(req.getUserId());
        entity.setType(req.getType());
        entity.setComment(req.getComment());
        entity.setStatus("pending");
        entity.setCreateTime(new Date());
        subjectRequestService.save(entity);
        return R.ok(entity);
    }

    @PostMapping("/process")
    public R<?> process(@RequestBody @Validated ProcessReq req) {
        SubjectRequest sr = subjectRequestService.getById(req.getId());
        if (sr == null) return R.error(40000, "申请不存在");
        if (!ALLOWED_STATUS.contains(req.getStatus())) return R.error(40000, "不支持的状态");
        if (FINAL_STATUS.contains(sr.getStatus())) return R.error(40000, "已完结的工单不可再次处理");
        sr.setStatus(req.getStatus());
        if (req.getHandlerId() != null) {
            sr.setHandlerId(req.getHandlerId());
        }
        sr.setResult(req.getResult());
        sr.setUpdateTime(new Date());
        subjectRequestService.updateById(sr);
        return R.okMsg("处理完成");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        subjectRequestService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class ApplyReq {
        @NotNull private Long userId;
        @NotBlank private String type;
        private String comment;
        public Long getUserId(){return userId;}
        public void setUserId(Long v){userId=v;}
        public String getType(){return type;}
        public void setType(String v){type=v;}
        public String getComment(){return comment;}
        public void setComment(String v){comment=v;}
    }

    public static class ProcessReq {
        @NotNull private Long id;
        @NotBlank private String status;
        private String result;
        public Long getId(){return id;}
        public void setId(Long v){id=v;}
        public Long getHandlerId(){return handlerId;}
        public void setHandlerId(Long v){handlerId=v;}
        public String getStatus(){return status;}
        public void setStatus(String v){status=v;}
        public String getResult(){return result;}
        public void setResult(String v){result=v;}
    }

    public static class IdReq {
        @NotNull private Long id;
        public Long getId(){return id;}
        public void setId(Long v){id=v;}
    }
}
