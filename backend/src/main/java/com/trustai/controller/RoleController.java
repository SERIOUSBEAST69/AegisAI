package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.Role;
import com.trustai.service.CurrentUserService;
import com.trustai.service.RoleService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Autowired
    private RoleService roleService;
    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/list")
    @PreAuthorize("@currentUserService.hasRole('ADMIN')")
    public R<List<Role>> list(@RequestParam(required = false) String name) {
        QueryWrapper<Role> qw = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            qw.like("name", name);
        }
        return R.ok(roleService.list(qw));
    }

    @PostMapping("/add")
    @PreAuthorize("@currentUserService.hasRole('ADMIN')")
    public R<?> add(@RequestBody Role role) {
        currentUserService.requireAdmin();
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());
        roleService.save(role);
        return R.okMsg("添加成功");
    }

    @PostMapping("/update")
    @PreAuthorize("@currentUserService.hasRole('ADMIN')")
    public R<?> update(@RequestBody Role role) {
        currentUserService.requireAdmin();
        role.setUpdateTime(new Date());
        roleService.updateById(role);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    @PreAuthorize("@currentUserService.hasRole('ADMIN')")
    public R<?> delete(@RequestBody IdReq req) {
        currentUserService.requireAdmin();
        roleService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}
