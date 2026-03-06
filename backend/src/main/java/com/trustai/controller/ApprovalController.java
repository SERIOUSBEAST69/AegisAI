package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.ApprovalRequest;
import com.trustai.service.ApprovalRequestService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/approval")
@Validated
public class ApprovalController {
    @Autowired private ApprovalRequestService approvalRequestService;

    private static final Set<String> APPROVE_STATUS = new HashSet<>(Arrays.asList("通过", "拒绝"));

    @GetMapping("/list")
    public R<List<ApprovalRequest>> list(@RequestParam(required = false) Long applicantId,
                                         @RequestParam(required = false) Long assetId) {
        QueryWrapper<ApprovalRequest> qw = new QueryWrapper<>();
        if (applicantId != null) qw.eq("applicant_id", applicantId);
        if (assetId != null) qw.eq("asset_id", assetId);
        return R.ok(approvalRequestService.list(qw));
    }

    @PostMapping("/apply")
    public R<?> apply(@RequestBody ApprovalRequest req) {
        req.setStatus("待审批");
        req.setCreateTime(new Date());
        req.setUpdateTime(new Date());
        approvalRequestService.save(req);
        return R.okMsg("提交成功");
    }

    @PostMapping("/approve")
    public R<?> approve(@RequestBody ApproveReq req) {
        ApprovalRequest ar = approvalRequestService.getById(req.getRequestId());
        if (ar == null) return R.error(40000, "申请不存在");
        if (!APPROVE_STATUS.contains(req.getStatus())) return R.error(40000, "不支持的状态");
        if (!"待审批".equals(ar.getStatus())) return R.error(40000, "该申请已处理");
        ar.setStatus(req.getStatus());
        ar.setApproverId(req.getApproverId());
        ar.setUpdateTime(new Date());
        approvalRequestService.updateById(ar);
        return R.okMsg("审批完成");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        approvalRequestService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class ApproveReq { public Long getRequestId(){return requestId;} public void setRequestId(Long id){this.requestId=id;} public Long getApproverId(){return approverId;} public void setApproverId(Long id){this.approverId=id;} public String getStatus(){return status;} public void setStatus(String s){this.status=s;} private Long requestId; private Long approverId; private String status; }

    public static class IdReq { @NotNull private Long id; public Long getId(){return id;} public void setId(Long id){this.id=id;} }
}
