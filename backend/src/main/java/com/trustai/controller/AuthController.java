package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.config.jwt.JwtUtil;
import com.trustai.entity.User;
import com.trustai.exception.BizException;
import com.trustai.service.UserService;
import com.trustai.utils.R;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/login")
    public R<?> login(@RequestBody LoginReq req) {
        List<User> users = userService.list(new QueryWrapper<User>().eq("username", req.getUsername()));
        // 若存在重复用户名，优先选择已加密密码的记录（避免未加密口令导致解码异常）
        User user = users.stream()
            .filter(u -> u.getPassword() != null && u.getPassword().startsWith("$2"))
            .findFirst()
            .orElse(users.isEmpty() ? null : users.get(0));
        log.info("login pick user id={}, pwdPref={}",
            user != null ? user.getId() : null,
            user != null && user.getPassword() != null ? user.getPassword().substring(0, Math.min(4, user.getPassword().length())) : null);
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
