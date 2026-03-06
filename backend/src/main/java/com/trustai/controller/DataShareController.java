package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.DataShareRequest;
import com.trustai.service.DataShareRequestService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/data-share")
@Validated
public class DataShareController {

    @Autowired
    private DataShareRequestService dataShareRequestService;

    private static final Set<String> APPROVE_STATUS = new HashSet<>(Arrays.asList("approved", "rejected"));

    @GetMapping("/list")
    public R<List<DataShareRequest>> list(@RequestParam(required = false) String status) {
        QueryWrapper<DataShareRequest> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        return R.ok(dataShareRequestService.list(qw));
    }

    @PostMapping("/apply")
    public R<?> apply(@RequestBody @Validated ApplyReq req) {
        DataShareRequest entity = new DataShareRequest();
        entity.setAssetId(req.getAssetId());
        entity.setApplicantId(req.getApplicantId());
        entity.setCollaborators(req.getCollaborators());
        entity.setReason(req.getReason());
        entity.setStatus("pending");
        entity.setCreateTime(new Date());
        dataShareRequestService.save(entity);
        return R.okMsg("提交成功");
    }

    @PostMapping("/approve")
    public R<?> approve(@RequestBody @Validated ApproveReq req) {
        DataShareRequest ds = dataShareRequestService.getById(req.getId());
        if (ds == null) return R.error(40000, "申请不存在");
        if (!APPROVE_STATUS.contains(req.getStatus())) return R.error(40000, "不支持的状态");
        if (!"pending".equalsIgnoreCase(ds.getStatus())) return R.error(40000, "该申请已处理");
        ds.setStatus(req.getStatus());
        ds.setApproverId(req.getApproverId());
        ds.setUpdateTime(new Date());
        dataShareRequestService.updateById(ds);
        return R.okMsg("处理完成");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        dataShareRequestService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class ApproveReq {
        @NotNull private Long id; @NotNull private Long approverId; @NotBlank private String status;
        public Long getId(){return id;} public void setId(Long id){this.id=id;}
        public Long getApproverId(){return approverId;} public void setApproverId(Long a){this.approverId=a;}
        public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    }
    public static class IdReq {
        @NotNull private Long id;
        public Long getId(){return id;} public void setId(Long id){this.id=id;}
    }
    public static class ApplyReq {
        @NotNull private Long assetId; @NotNull private Long applicantId; @NotBlank private String reason;
        private String collaborators;
        public Long getAssetId(){return assetId;} public void setAssetId(Long v){assetId=v;}
        public Long getApplicantId(){return applicantId;} public void setApplicantId(Long v){applicantId=v;}
        public String getReason(){return reason;} public void setReason(String v){reason=v;}
        public String getCollaborators(){return collaborators;} public void setCollaborators(String v){collaborators=v;}
    }
}
