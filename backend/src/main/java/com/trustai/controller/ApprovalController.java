package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.ApprovalRequest;
import com.trustai.service.ApprovalRequestService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {
    @Autowired private ApprovalRequestService approvalRequestService;

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
        approvalRequestService.save(req);
        return R.okMsg("提交成功");
    }

    @PostMapping("/approve")
    public R<?> approve(@RequestBody ApproveReq req) {
        ApprovalRequest ar = approvalRequestService.getById(req.getRequestId());
        if (ar == null) return R.error(40000, "申请不存在");
        ar.setStatus(req.getStatus());
        ar.setApproverId(req.getApproverId());
        approvalRequestService.updateById(ar);
        return R.okMsg("审批完成");
    }

    public static class ApproveReq { public Long getRequestId(){return requestId;} public void setRequestId(Long id){this.requestId=id;} public Long getApproverId(){return approverId;} public void setApproverId(Long id){this.approverId=id;} public String getStatus(){return status;} public void setStatus(String s){this.status=s;} private Long requestId; private Long approverId; private String status; }
}
