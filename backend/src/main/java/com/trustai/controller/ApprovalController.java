package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.ApprovalRequest;
import com.trustai.entity.Role;
import com.trustai.entity.User;
import com.trustai.exception.BizException;
import com.trustai.service.ApprovalRequestService;
import com.trustai.service.CurrentUserService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
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
    @Autowired private CurrentUserService currentUserService;

    private static final Set<String> APPROVE_STATUS = new HashSet<>(Arrays.asList("通过", "拒绝"));

    @GetMapping("/list")
    public R<List<ApprovalRequest>> list(@RequestParam(required = false) Long applicantId,
                                         @RequestParam(required = false) Long assetId) {
        User currentUser = currentUserService.requireCurrentUser();
        QueryWrapper<ApprovalRequest> qw = new QueryWrapper<>();
        if (isApprovalOperator(currentUser)) {
            if (applicantId != null) qw.eq("applicant_id", applicantId);
        } else {
            qw.eq("applicant_id", currentUser.getId());
        }
        if (assetId != null) qw.eq("asset_id", assetId);
        return R.ok(approvalRequestService.list(qw));
    }

    @PostMapping("/apply")
    public R<?> apply(@RequestBody ApprovalRequest req) {
        User currentUser = currentUserService.requireCurrentUser();
        req.setApplicantId(currentUser.getId());
        req.setApproverId(null);
        req.setStatus(null);
        req.setTaskId(null);
        req.setProcessInstanceId(null);
        approvalRequestService.startApproval(req);
        return R.okMsg("提交成功");
    }

    @PostMapping("/approve")
    public R<?> approve(@RequestBody ApproveReq req) {
        if (!APPROVE_STATUS.contains(req.getStatus())) return R.error(40000, "不支持的状态");
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN");
        User currentUser = currentUserService.requireCurrentUser();
        approvalRequestService.approve(req.getRequestId(), currentUser.getId(), req.getStatus());
        return R.okMsg("审批完成");
    }

    @GetMapping("/todo")
    public R<List<ApprovalRequest>> todo() {
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN");
        User currentUser = currentUserService.requireCurrentUser();
        return R.ok(approvalRequestService.todo(currentUser.getId()));
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        User currentUser = currentUserService.requireCurrentUser();
        ApprovalRequest approval = approvalRequestService.getById(req.getId());
        if (approval == null) {
            throw new BizException(40000, "申请不存在");
        }
        if (!isApprovalOperator(currentUser) && !currentUser.getId().equals(approval.getApplicantId())) {
            throw new BizException(40300, "仅可删除本人申请");
        }
        approvalRequestService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    private boolean isApprovalOperator(User user) {
        Role role = currentUserService.getCurrentRole(user);
        if (role == null || role.getCode() == null) {
            return false;
        }
        return Arrays.asList("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN").contains(role.getCode().toUpperCase());
    }

    public static class ApproveReq { public Long getRequestId(){return requestId;} public void setRequestId(Long id){this.requestId=id;} public String getStatus(){return status;} public void setStatus(String s){this.status=s;} private Long requestId; private String status; }

    public static class IdReq { @NotNull private Long id; public Long getId(){return id;} public void setId(Long id){this.id=id;} }
}
