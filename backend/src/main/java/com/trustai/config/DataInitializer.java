package com.trustai.config;

import com.trustai.entity.Role;
import com.trustai.entity.User;
import com.trustai.service.RoleService;
import com.trustai.service.UserService;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 简单的启动数据填充：若系统无用户与角色，初始化一个管理员账号。
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Map<String, String> roles = new LinkedHashMap<>();
        roles.put("ADMIN", "治理管理员");
        roles.put("EXECUTIVE", "管理层");
        roles.put("SECOPS", "安全运维");
        roles.put("DATA_ADMIN", "数据管理员");
        roles.put("AI_BUILDER", "AI应用开发者");
        roles.put("SCHOOL_ADMIN", "学校管理员");
        roles.put("BUSINESS_OWNER", "业务负责人");
        roles.put("EMPLOYEE", "普通员工");

        roles.forEach(this::ensureRole);

        ensureUser("admin", "admin123", "平台管理员", "ADMIN", "enterprise", "治理中心", "13800138000", "admin@aegisai.com", "password", "wx_admin");
        ensureUser("exec.demo", "demo1234", "经营负责人", "EXECUTIVE", "enterprise", "经营管理部", "13800138001", "exec@aegisai.com", "password", "wx_exec_demo");
        ensureUser("secops.demo", "demo1234", "安全运维负责人", "SECOPS", "enterprise", "安全运营中心", "13800138002", "secops@aegisai.com", "password", "wx_secops_demo");
        ensureUser("data.demo", "demo1234", "数据管理员", "DATA_ADMIN", "enterprise", "数据治理部", "13800138003", "data@aegisai.com", "password", "wx_data_demo");
        ensureUser("builder.demo", "demo1234", "AI应用开发者", "AI_BUILDER", "ai-team", "模型平台组", "13800138004", "builder@aegisai.com", "password", "wx_builder_demo");
        ensureUser("school.demo", "demo1234", "校园数据管理员", "SCHOOL_ADMIN", "school", "智慧校园中心", "13800138005", "school@aegisai.com", "password", "wx_school_demo");
        ensureUser("biz.demo", "demo1234", "业务负责人", "BUSINESS_OWNER", "enterprise", "业务创新部", "13800138006", "biz@aegisai.com", "password", "wx_biz_demo");
        ensureUser("employee.demo", "demo1234", "普通员工", "EMPLOYEE", "enterprise", "业务一线", "13800138007", "employee@aegisai.com", "password", "wx_employee_demo");
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
        if (isNew) {
            user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setCreateTime(new Date());
        }
        // 对于已存在的用户，不修改密码
        user.setRealName(realName);
        user.setNickname(realName);
        user.setRoleId(role == null ? null : role.getId());
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
            user.setPassword(null); // 确保更新时不修改现有用户密码（updateById 默认跳过 null 字段）
            userService.updateById(user);
        }
    }
 }