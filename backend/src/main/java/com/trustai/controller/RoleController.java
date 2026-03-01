package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.Role;
import com.trustai.service.RoleService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping("/list")
    public R<List<Role>> list(@RequestParam(required = false) String name) {
        QueryWrapper<Role> qw = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            qw.like("name", name);
        }
        return R.ok(roleService.list(qw));
    }

    @PostMapping("/add")
    public R<?> add(@RequestBody Role role) {
        roleService.save(role);
        return R.okMsg("添加成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody Role role) {
        roleService.updateById(role);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody IdReq req) {
        roleService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}
