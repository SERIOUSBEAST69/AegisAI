package com.trustai.config;

import com.trustai.entity.Role;
import com.trustai.entity.User;
import com.trustai.service.RoleService;
import com.trustai.service.UserService;
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
        if (userService.count() > 0) {
            return;
        }
        // 创建默认角色
        Role adminRole = new Role();
        adminRole.setName("管理员");
        adminRole.setCode("ADMIN");
        adminRole.setDescription("系统默认管理员角色");
        roleService.save(adminRole);

        // 创建默认管理员用户 admin / admin123
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRealName("平台管理员");
        admin.setRoleId(adminRole.getId());
        admin.setStatus(1);
        userService.save(admin);
    }
}
