package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.config.jwt.JwtUtil;
import com.trustai.entity.User;
import com.trustai.exception.BizException;
import com.trustai.service.UserService;
import com.trustai.utils.R;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/login")
    public R<?> login(@RequestBody LoginReq req) {
        User user = userService.getOne(new QueryWrapper<User>().eq("username", req.getUsername()));
        if (user == null || user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(40100, "用户不存在或已禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(40100, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        user.setPassword(null);
        return R.ok(new LoginResp(token, user));
    }

    @Data
    public static class LoginReq { private String username; private String password; }
    @Data
    public static class LoginResp { private final String token; private final User user; }
}
