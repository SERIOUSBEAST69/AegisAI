package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.dto.ChangePasswordDTO;
import com.trustai.dto.UserProfileDTO;
import com.trustai.dto.UserUpdateDTO;
import com.trustai.entity.Role;
import com.trustai.entity.User;
import com.trustai.exception.BizException;
import com.trustai.service.CurrentUserService;
import com.trustai.service.UserService;
import com.trustai.utils.R;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CurrentUserService currentUserService;

    @GetMapping("/list")
    @PreAuthorize("@currentUserService.hasRole('ADMIN')")
    public R<List<User>> list(@RequestParam(required = false) String username) {
        currentUserService.requireAdmin();
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (username != null && !username.isEmpty()) qw.like("username", username);
        List<User> list = userService.list(qw);
        list.forEach(u -> u.setPassword(null));
        return R.ok(list);
    }

    @PostMapping("/register")
    @PreAuthorize("@currentUserService.hasRole('ADMIN')")
    public R<?> register(@RequestBody User user) {
        currentUserService.requireAdmin();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        userService.save(user);
        return R.okMsg("注册成功");
    }

    @PostMapping("/update")
    @PreAuthorize("@currentUserService.hasRole('ADMIN')")
    public R<?> update(@RequestBody User user) {
        currentUserService.requireAdmin();
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setUpdateTime(new Date());
        userService.updateById(user);
        return R.okMsg("更新成功");
    }

    @PostMapping("/delete")
    @PreAuthorize("@currentUserService.hasRole('ADMIN')")
    public R<?> delete(@RequestBody IdReq req) {
        currentUserService.requireAdmin();
        userService.removeById(req.getId());
        return R.okMsg("删除成功");
    }

    public static class IdReq { public Long getId(){return id;} public void setId(Long id){this.id=id;} private Long id; }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public R<UserProfileDTO> profile() {
        User user = currentUserService.requireCurrentUser();
        return R.ok(toProfile(user));
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public R<UserProfileDTO> updateProfile(@ModelAttribute UserUpdateDTO req) {
        User user = currentUserService.requireCurrentUser();
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getRealName() != null) user.setRealName(req.getRealName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        MultipartFile avatar = req.getAvatar();
        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatar(storeAvatar(avatar));
        }
        user.setUpdateTime(new Date());
        userService.updateById(user);
        return R.ok(toProfile(user));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public R<?> changePassword(@Validated @RequestBody ChangePasswordDTO req) {
        User user = currentUserService.requireCurrentUser();
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BizException(40000, "旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdateTime(new Date());
        userService.updateById(user);
        return R.okMsg("密码已更新");
    }

    private UserProfileDTO toProfile(User user) {
        Role role = currentUserService.getCurrentRole(user);
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .nickname(user.getNickname())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .roleName(role == null ? null : role.getName())
                .roleCode(role == null ? null : role.getCode())
                .lastActiveAt(user.getUpdateTime() == null ? null : user.getUpdateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }

    private String storeAvatar(MultipartFile file) {
        try {
            String original = file.getOriginalFilename();
            String ext = original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String filename = UUID.randomUUID() + ext;
            Path dir = Paths.get("uploads");
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return dir.resolve(filename).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new BizException(50000, "头像上传失败: " + e.getMessage());
        }
    }
}
