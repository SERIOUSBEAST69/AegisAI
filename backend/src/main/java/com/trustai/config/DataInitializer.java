package com.trustai.config;

import com.trustai.entity.Role;
import com.trustai.entity.User;
import com.trustai.service.RoleService;
import com.trustai.service.UserService;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 简单的启动数据填充：若系统无用户与角色，初始化一个管理员账号。
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Map<String, String> ROLE_LABELS = new LinkedHashMap<>();

    static {
        ROLE_LABELS.put("ADMIN", "治理管理员");
        ROLE_LABELS.put("EXECUTIVE", "管理层");
        ROLE_LABELS.put("SECOPS", "安全运维");
        ROLE_LABELS.put("DATA_ADMIN", "数据管理员");
        ROLE_LABELS.put("AI_BUILDER", "AI应用开发者");
        ROLE_LABELS.put("BUSINESS_OWNER", "业务负责人");
        ROLE_LABELS.put("EMPLOYEE", "普通员工");
    }

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ROLE_LABELS.forEach(this::ensureRole);

        ensureUser("admin", "admin123", "平台管理员", "ADMIN", "enterprise", "治理中心", "13800138000", "admin@aegisai.com", "password", "wx_admin");
        ensureUser("exec.demo", "demo1234", "经营负责人", "EXECUTIVE", "enterprise", "经营管理部", "13800138001", "exec@aegisai.com", "password", "wx_exec_demo");
        ensureUser("secops.demo", "demo1234", "安全运维负责人", "SECOPS", "enterprise", "安全运营中心", "13800138002", "secops@aegisai.com", "password", "wx_secops_demo");
        ensureUser("data.demo", "demo1234", "数据管理员", "DATA_ADMIN", "enterprise", "数据治理部", "13800138003", "data@aegisai.com", "password", "wx_data_demo");
        ensureUser("builder.demo", "demo1234", "AI应用开发者", "AI_BUILDER", "ai-team", "模型平台组", "13800138004", "builder@aegisai.com", "password", "wx_builder_demo");
        ensureUser("biz.demo", "demo1234", "业务负责人", "BUSINESS_OWNER", "enterprise", "业务创新部", "13800138006", "biz@aegisai.com", "password", "wx_biz_demo");
        ensureUser("employee.demo", "demo1234", "普通员工", "EMPLOYEE", "enterprise", "业务一线", "13800138007", "employee@aegisai.com", "password", "wx_employee_demo");

        cleanupDeprecatedSchoolIdentity();
    }

    private void ensureRole(String code, String name) {
        if (roleService.lambdaQuery().eq(Role::getCode, code).count() > 0) {
            return;
        }
        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setDescription("系统默认角色: " + name);
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());
        roleService.save(role);
    }

    private void ensureUser(String username, String password, String realName, String roleCode,
                            String organizationType, String department, String phone, String email, String loginType,
                            String wechatOpenId) {
        Role role = roleService.lambdaQuery().eq(Role::getCode, roleCode).one();
        User user = userService.lambdaQuery().eq(User::getUsername, username).one();
        boolean isNew = user == null;
        boolean shouldResetPassword = false;
        if (isNew) {
            user = new User();
            user.setUsername(username);
            user.setCreateTime(new Date());
            shouldResetPassword = true;
        } else if (!isBcryptHash(user.getPassword())) {
            shouldResetPassword = true;
        }

        if (shouldResetPassword) {
            user.setPassword(passwordEncoder.encode(password));
        }

        // 对于已存在的用户，不修改密码
        user.setRealName(realName);
        user.setNickname(realName);
        user.setRoleId(role == null ? null : role.getId());
        user.setDeviceId(username + "-device");
        user.setOrganizationType(organizationType);
        user.setDepartment(department);
        user.setPhone(phone);
        user.setEmail(email);
        user.setLoginType(loginType);
        user.setWechatOpenId(wechatOpenId);
        user.setStatus(1);
        user.setUpdateTime(new Date());
        if (isNew) {
            userService.save(user);
        } else {
            if (!shouldResetPassword) {
                user.setPassword(null); // 确保更新时不修改现有用户密码（updateById 默认跳过 null 字段）
            }
            userService.updateById(user);
        }
    }

    private void cleanupDeprecatedSchoolIdentity() {
        userService.lambdaUpdate().eq(User::getUsername, "school.demo").remove();

        Role schoolRole = roleService.lambdaQuery().eq(Role::getCode, "SCHOOL_ADMIN").one();
        if (schoolRole == null) {
            return;
        }

        Role fallbackRole = roleService.lambdaQuery().eq(Role::getCode, "DATA_ADMIN").one();
        List<User> users = userService.lambdaQuery().eq(User::getRoleId, schoolRole.getId()).list();
        for (User user : users) {
            user.setRoleId(fallbackRole == null ? null : fallbackRole.getId());
            user.setUpdateTime(new Date());
            user.setPassword(null);
            userService.updateById(user);
        }

        roleService.removeById(schoolRole.getId());
    }

    private boolean isBcryptHash(String value) {
        return StringUtils.hasText(value) && value.startsWith("$2");
    }
}