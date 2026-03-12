package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.CompliancePolicy;
import com.trustai.service.CurrentUserService;
import com.trustai.service.CompliancePolicyService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {
    @Autowired private CompliancePolicyService compliancePolicyService;
    @Autowired private CurrentUserService currentUserService;

    @GetMapping("/list")
    public R<List<CompliancePolicy>> list(@RequestParam(required = false) String name) {
        QueryWrapper<CompliancePolicy> qw = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) qw.like("name", name);
        return R.ok(compliancePolicyService.list(qw));
    }

    @PostMapping("/save")
    public R<?> save(@RequestBody CompliancePolicy policy) {
        currentUserService.requireAnyRole("ADMIN", "DATA_ADMIN", "SECOPS");
        policy.setUpdateTime(new Date());
        if (policy.getId() == null) {
            policy.setCreateTime(new Date());
            compliancePolicyService.save(policy);
        } else {
            compliancePolicyService.updateById(policy);
        }
        return R.okMsg("保存成功");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody IdReq req) {
        currentUserService.requireAnyRole("ADMIN", "DATA_ADMIN", "SECOPS");
        compliancePolicyService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq { public Long getId(){return id;} public void setId(Long id){this.id=id;} private Long id; }
}
