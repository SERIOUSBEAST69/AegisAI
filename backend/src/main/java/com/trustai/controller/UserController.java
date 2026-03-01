package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.entity.User;
import com.trustai.service.UserService;
import com.trustai.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public R<List<User>> list(@RequestParam(required = false) String username) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (username != null && !username.isEmpty()) qw.like("username", username);
        List<User> list = userService.list(qw);
        list.forEach(u -> u.setPassword(null));
        return R.ok(list);
    }

    @PostMapping("/register")
    public R<?> register(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.save(user);
        return R.okMsg("注册成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userService.updateById(user);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestBody IdReq req) {
        userService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq { public Long getId(){return id;} public void setId(Long id){this.id=id;} private Long id; }
}
