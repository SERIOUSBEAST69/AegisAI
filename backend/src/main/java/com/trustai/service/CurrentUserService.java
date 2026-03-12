package com.trustai.service;

import com.trustai.entity.Role;
import com.trustai.entity.User;
import com.trustai.exception.BizException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserService userService;
    private final RoleService roleService;

    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BizException(40100, "未登录");
        }
        User user = userService.lambdaQuery().eq(User::getUsername, auth.getName()).one();
        if (user == null) {
            throw new BizException(40100, "用户不存在");
        }
        return user;
    }

    public void requireAdmin() {
        User user = requireCurrentUser();
        if (user.getRoleId() == null) {
            throw new BizException(40300, "仅管理员可操作");
        }
        Role role = roleService.getById(user.getRoleId());
        boolean isAdmin = "admin".equalsIgnoreCase(user.getUsername())
                || (role != null && ("ADMIN".equalsIgnoreCase(role.getCode()) || role.getName().contains("管理员")));
        if (!isAdmin) {
            throw new BizException(40300, "仅管理员可操作");
        }
    }

    public void requireAnyRole(String... roleCodes) {
        User user = requireCurrentUser();
        Role role = getCurrentRole(user);
        if (role == null || !StringUtils.hasText(role.getCode())) {
            throw new BizException(40300, "当前账号未分配身份");
        }

        boolean allowed = Arrays.stream(roleCodes)
            .filter(StringUtils::hasText)
            .anyMatch(code -> code.equalsIgnoreCase(role.getCode()));
        if (!allowed) {
            throw new BizException(40300, "当前身份无权执行该操作");
        }
    }

    public Role getCurrentRole(User user) {
        return user.getRoleId() == null ? null : roleService.getById(user.getRoleId());
    }
}
