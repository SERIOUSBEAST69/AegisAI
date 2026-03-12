package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.DataAsset;
import com.trustai.entity.DataShareRequest;
import com.trustai.entity.Role;
import com.trustai.entity.User;
import com.trustai.exception.BizException;
import com.trustai.service.CurrentUserService;
import com.trustai.service.DataAssetService;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/data-share")
@Validated
public class DataShareController {

    @Autowired
    private DataShareRequestService dataShareRequestService;

    @Autowired
    private DataAssetService dataAssetService;

    @Autowired
    private CurrentUserService currentUserService;

    private static final Set<String> APPROVE_STATUS = new HashSet<>(Arrays.asList("approved", "rejected"));

    @GetMapping("/list")
    public R<List<DataShareRequest>> list(@RequestParam(required = false) String status) {
        User currentUser = currentUserService.requireCurrentUser();
        QueryWrapper<DataShareRequest> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        if (!isShareOperator(currentUser)) {
            qw.eq("applicant_id", currentUser.getId());
        }
        return R.ok(dataShareRequestService.list(qw));
    }

    @PostMapping("/apply")
    public R<?> apply(@RequestBody @Validated ApplyReq req) {
        User currentUser = currentUserService.requireCurrentUser();
        DataShareRequest entity = new DataShareRequest();
        entity.setAssetId(req.getAssetId());
        entity.setApplicantId(currentUser.getId());
        entity.setCollaborators(req.getCollaborators());
        entity.setReason(req.getReason());
        entity.setStatus("pending");
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        dataShareRequestService.save(entity);
        return R.okMsg("提交成功");
    }

    @PostMapping("/approve")
    public R<?> approve(@RequestBody @Validated ApproveReq req) {
        currentUserService.requireAnyRole("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN");
        User currentUser = currentUserService.requireCurrentUser();
        DataShareRequest ds = dataShareRequestService.getById(req.getId());
        if (ds == null) return R.error(40000, "申请不存在");
        if (!APPROVE_STATUS.contains(req.getStatus())) return R.error(40000, "不支持的状态");
        if (!"pending".equalsIgnoreCase(ds.getStatus())) return R.error(40000, "该申请已处理");
        ds.setStatus(req.getStatus());
        ds.setApproverId(currentUser.getId());
        if ("approved".equalsIgnoreCase(req.getStatus())) {
            ds.setShareToken(generateToken());
            ds.setExpireTime(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));
        } else {
            ds.setShareToken(null);
            ds.setExpireTime(null);
        }
        ds.setUpdateTime(new Date());
        dataShareRequestService.updateById(ds);
        return R.okMsg("处理完成");
    }

    @GetMapping("/access")
    public R<?> access(@RequestParam String token) {
        QueryWrapper<DataShareRequest> qw = new QueryWrapper<>();
        qw.eq("share_token", token);
        DataShareRequest ds = dataShareRequestService.getOne(qw);
        if (ds == null) return R.error(40000, "链接无效或已失效");
        if (!"approved".equalsIgnoreCase(ds.getStatus())) return R.error(40000, "链接未被批准");
        if (ds.getExpireTime() != null && ds.getExpireTime().before(new Date())) return R.error(40000, "链接已过期");

        DataAsset asset = dataAssetService.getById(ds.getAssetId());
        if (asset == null) return R.error(40400, "数据资产不存在");

        return R.ok(maskAsset(asset));
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody @Validated IdReq req) {
        User currentUser = currentUserService.requireCurrentUser();
        DataShareRequest request = dataShareRequestService.getById(req.getId());
        if (request == null) {
            throw new BizException(40000, "申请不存在");
        }
        if (!isShareOperator(currentUser) && !currentUser.getId().equals(request.getApplicantId())) {
            throw new BizException(40300, "仅可删除本人申请");
        }
        dataShareRequestService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class ApproveReq {
        @NotNull private Long id; @NotBlank private String status;
        public Long getId(){return id;} public void setId(Long id){this.id=id;}
        public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    }
    public static class IdReq {
        @NotNull private Long id;
        public Long getId(){return id;} public void setId(Long id){this.id=id;}
    }
    public static class ApplyReq {
        @NotNull private Long assetId; @NotBlank private String reason;
        private String collaborators;
        public Long getAssetId(){return assetId;} public void setAssetId(Long v){assetId=v;}
        public String getReason(){return reason;} public void setReason(String v){reason=v;}
        public String getCollaborators(){return collaborators;} public void setCollaborators(String v){collaborators=v;}
    }

    private boolean isShareOperator(User user) {
        Role role = currentUserService.getCurrentRole(user);
        if (role == null || role.getCode() == null) {
            return false;
        }
        return Arrays.asList("ADMIN", "SECOPS", "DATA_ADMIN", "EXECUTIVE", "SCHOOL_ADMIN").contains(role.getCode().toUpperCase());
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private Map<String, Object> maskAsset(DataAsset asset) {
        return Map.of(
                "assetId", asset.getId(),
                "name", asset.getName(),
                "type", asset.getType(),
                "sensitivityLevel", asset.getSensitivityLevel(),
                "location", mask(asset.getLocation())
        );
    }

    private String mask(String value) {
        if (value == null || value.isEmpty()) return value;
        if (value.length() <= 4) return "**" + value.charAt(value.length() - 1);
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
