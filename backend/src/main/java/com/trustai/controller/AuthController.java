package com.trustai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.trustai.config.jwt.JwtUtil;
import com.trustai.dto.UserProfileDTO;
import com.trustai.entity.Role;
import com.trustai.entity.User;
import com.trustai.exception.BizException;
import com.trustai.service.AuthVerificationService;
import com.trustai.service.CurrentUserService;
import com.trustai.service.RoleService;
import com.trustai.service.UserService;
import com.trustai.utils.R;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final List<DemoAccountSeed> DEMO_ACCOUNT_SEEDS = List.of(
        new DemoAccountSeed("admin", "admin123", "平台管理员", "ADMIN", "enterprise", "治理中心", "13800138000", "admin@aegisai.com", "wx_admin"),
        new DemoAccountSeed("exec.demo", "demo1234", "经营负责人", "EXECUTIVE", "enterprise", "经营管理部", "13800138001", "exec@aegisai.com", "wx_exec_demo"),
        new DemoAccountSeed("secops.demo", "demo1234", "安全运维负责人", "SECOPS", "enterprise", "安全运营中心", "13800138002", "secops@aegisai.com", "wx_secops_demo"),
        new DemoAccountSeed("data.demo", "demo1234", "数据管理员", "DATA_ADMIN", "enterprise", "数据治理部", "13800138003", "data@aegisai.com", "wx_data_demo"),
        new DemoAccountSeed("builder.demo", "demo1234", "AI应用开发者", "AI_BUILDER", "ai-team", "模型平台组", "13800138004", "builder@aegisai.com", "wx_builder_demo"),
        new DemoAccountSeed("school.demo", "demo1234", "校园数据管理员", "SCHOOL_ADMIN", "school", "智慧校园中心", "13800138005", "school@aegisai.com", "wx_school_demo"),
        new DemoAccountSeed("biz.demo", "demo1234", "业务负责人", "BUSINESS_OWNER", "enterprise", "业务创新部", "13800138006", "biz@aegisai.com", "wx_biz_demo"),
        new DemoAccountSeed("employee.demo", "demo1234", "普通员工", "EMPLOYEE", "enterprise", "业务一线", "13800138007", "employee@aegisai.com", "wx_employee_demo")
    );

    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private RoleService roleService;
    @Autowired private AuthVerificationService authVerificationService;

    @PostMapping("/login")
    public R<?> login(@RequestBody LoginReq req) {
        ensureDemoUserByUsername(req.getUsername());
        User user = findEnabledUserByUsername(req.getUsername());
        log.info("login pick user id={}, pwdPref={}",
            user != null ? user.getId() : null,
            user != null && user.getPassword() != null ? user.getPassword().substring(0, Math.min(4, user.getPassword().length())) : null);
        if (user == null) {
            throw new BizException(40100, "用户不存在或已禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(40100, "用户名或密码错误");
        }
        user.setLoginType("password");
        user.setUpdateTime(new Date());
        userService.updateById(user);
        return R.ok(buildSession(user, true));
    }

    @PostMapping("/login-phone")
    public R<?> loginByPhone(@RequestBody PhoneLoginReq req) {
        authVerificationService.verifyPhoneCode(req.getPhone(), req.getCode());
        ensureDemoUserByPhone(req.getPhone());
        User user = findEnabledUserByPhone(req.getPhone());
        if (user == null) {
            throw new BizException(40100, "手机号未注册或已禁用");
        }
        user.setLoginType("phone");
        user.setUpdateTime(new Date());
        userService.updateById(user);
        return R.ok(buildSession(user, true));
    }

    @PostMapping("/login-wechat")
    public R<?> loginByWechat(@RequestBody WechatLoginReq req) {
        String openId = normalizeWechatOpenId(req.getWechatOpenId(), req.getNickname());
        ensureDemoUserByWechat(openId);
        User user = findEnabledUserByWechat(openId);
        if (user == null) {
            RegisterReq registerReq = new RegisterReq();
            registerReq.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : "微信用户");
            registerReq.setRealName(registerReq.getNickname());
            registerReq.setRoleCode(StringUtils.hasText(req.getRoleCode()) ? req.getRoleCode() : "BUSINESS_OWNER");
            registerReq.setOrganizationType(StringUtils.hasText(req.getOrganizationType()) ? req.getOrganizationType() : "enterprise");
            registerReq.setDepartment(StringUtils.hasText(req.getDepartment()) ? req.getDepartment() : "微信快速接入");
            registerReq.setPhone(req.getPhone());
            registerReq.setWechatOpenId(openId);
            user = createUser(registerReq, "wechat");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(40100, "用户已禁用");
        }
        user.setLoginType("wechat");
        user.setUpdateTime(new Date());
        userService.updateById(user);
        return R.ok(buildSession(user, true));
    }

    @PostMapping("/register")
    public R<?> register(@RequestBody RegisterReq req) {
        User user = createUser(req, StringUtils.hasText(req.getLoginType()) ? req.getLoginType() : inferLoginType(req));
        return R.ok(buildSession(user, true));
    }

    @PostMapping("/phone-code")
    public R<?> sendPhoneCode(@RequestBody PhoneCodeReq req) {
        Map<String, String> payload = new LinkedHashMap<>();
        AuthVerificationService.PhoneCodePayload issued = authVerificationService.issuePhoneCode(req.getPhone());
        payload.put("phone", issued.phone());
        payload.put("expiresAt", String.valueOf(issued.expiresAt()));
        payload.put("codeHint", issued.developmentMode() ? issued.code() : "");
        payload.put("message", issued.developmentMode() ? "验证码已生成，可用于本地联调" : "验证码已发送");
        return R.ok(payload);
    }

    @GetMapping("/registration-options")
    public R<?> registrationOptions() {
        List<Map<String, String>> identities = List.of(
            option("ADMIN", "治理管理员"),
            option("EXECUTIVE", "管理层"),
            option("SECOPS", "安全运维"),
            option("DATA_ADMIN", "数据管理员"),
            option("AI_BUILDER", "AI应用开发者"),
            option("SCHOOL_ADMIN", "学校管理员"),
            option("BUSINESS_OWNER", "业务负责人"),
            option("EMPLOYEE", "普通员工")
        );
        List<Map<String, String>> organizations = List.of(
            option("enterprise", "企业"),
            option("school", "学校"),
            option("ai-team", "AI应用团队"),
            option("public-sector", "政企/公共机构")
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("identities", identities);
        result.put("organizations", organizations);
        return R.ok(result);
    }

    @GetMapping("/me")
    public R<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BizException(40100, "未登录或令牌失效");
        }
        User user = currentUserService.requireCurrentUser();
        return R.ok(buildSession(user, false));
    }

    @PostMapping("/logout")
    public R<?> logout() {
        return R.okMsg("退出成功");
    }

    private SessionResp buildSession(User user, boolean includeToken) {
        String token = includeToken ? jwtUtil.generateToken(user.getUsername(), user.getId()) : null;
        user.setPassword(null);
        return new SessionResp(token, toProfile(user), true, System.currentTimeMillis());
    }

    private User findEnabledUserByUsername(String username) {
        List<User> users = userService.list(new QueryWrapper<User>().eq("username", username));
        return pickEnabledUser(users);
    }

    private User findEnabledUserByPhone(String phone) {
        List<User> users = userService.list(new QueryWrapper<User>().eq("phone", phone));
        return pickEnabledUser(users);
    }

    private User findEnabledUserByWechat(String wechatOpenId) {
        List<User> users = userService.list(new QueryWrapper<User>().eq("wechat_open_id", wechatOpenId));
        return pickEnabledUser(users);
    }

    private User pickEnabledUser(List<User> users) {
        User user = users.stream()
            .filter(candidate -> candidate.getStatus() == null || candidate.getStatus() != 0)
            .filter(candidate -> candidate.getPassword() != null && candidate.getPassword().startsWith("$2"))
            .findFirst()
            .orElseGet(() -> users.stream()
                .filter(candidate -> candidate.getStatus() == null || candidate.getStatus() != 0)
                .findFirst()
                .orElse(null));
        if (user == null) {
            return null;
        }
        return user;
    }

    private void ensureDemoUserByUsername(String username) {
        ensureDemoUser(DEMO_ACCOUNT_SEEDS.stream().filter(seed -> seed.username().equals(username)).findFirst().orElse(null));
    }

    private void ensureDemoUserByPhone(String phone) {
        ensureDemoUser(DEMO_ACCOUNT_SEEDS.stream().filter(seed -> seed.phone().equals(phone)).findFirst().orElse(null));
    }

    private void ensureDemoUserByWechat(String wechatOpenId) {
        ensureDemoUser(DEMO_ACCOUNT_SEEDS.stream().filter(seed -> seed.wechatOpenId().equals(wechatOpenId)).findFirst().orElse(null));
    }

    private void ensureDemoUser(DemoAccountSeed seed) {
        if (seed == null) {
            return;
        }
        Role role = roleService.lambdaQuery().eq(Role::getCode, seed.roleCode()).one();
        if (role == null) {
            return;
        }

        User user = userService.lambdaQuery().eq(User::getUsername, seed.username()).one();
        boolean isNew = user == null;
        if (isNew) {
            user = new User();
            user.setUsername(seed.username());
            user.setCreateTime(new Date());
        }

        user.setUsername(seed.username());
        user.setRealName(seed.realName());
        user.setNickname(seed.realName());
        user.setRoleId(role.getId());
        user.setOrganizationType(seed.organizationType());
        user.setDepartment(seed.department());
        user.setPhone(seed.phone());
        user.setEmail(seed.email());
        user.setLoginType("password");
        user.setWechatOpenId(seed.wechatOpenId());
        user.setStatus(1);
        user.setUpdateTime(new Date());
        if (isNew || !StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(seed.password()));
        }

        if (isNew) {
            userService.save(user);
        } else {
            userService.updateById(user);
        }
    }

    private User createUser(RegisterReq req, String loginType) {
        if (!StringUtils.hasText(req.getRoleCode())) {
            throw new BizException(40000, "请选择身份");
        }
        Role role = roleService.lambdaQuery().eq(Role::getCode, req.getRoleCode()).one();
        if (role == null) {
            throw new BizException(40000, "身份不存在，请联系管理员");
        }

        String username = resolveUsername(req, loginType);
        if (userService.lambdaQuery().eq(User::getUsername, username).count() > 0) {
            throw new BizException(40000, "用户名已存在");
        }
        if (StringUtils.hasText(req.getPhone()) && userService.lambdaQuery().eq(User::getPhone, req.getPhone()).count() > 0) {
            throw new BizException(40000, "手机号已注册");
        }

        String wechatOpenId = StringUtils.hasText(req.getWechatOpenId()) ? normalizeWechatOpenId(req.getWechatOpenId(), req.getNickname()) : null;
        if (StringUtils.hasText(wechatOpenId) && userService.lambdaQuery().eq(User::getWechatOpenId, wechatOpenId).count() > 0) {
            throw new BizException(40000, "微信身份已绑定");
        }

        if ("phone".equals(loginType)) {
            authVerificationService.verifyPhoneCode(req.getPhone(), req.getPhoneCode());
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(resolvePassword(req, loginType)));
        user.setRealName(StringUtils.hasText(req.getRealName()) ? req.getRealName() : req.getNickname());
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : req.getRealName());
        user.setRoleId(role.getId());
        user.setDepartment(req.getDepartment());
        user.setOrganizationType(req.getOrganizationType());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setLoginType(loginType);
        user.setWechatOpenId(wechatOpenId);
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        userService.save(user);
        return user;
    }

    private String resolveUsername(RegisterReq req, String loginType) {
        if (StringUtils.hasText(req.getUsername())) {
            return req.getUsername().trim();
        }
        if ("phone".equals(loginType) && StringUtils.hasText(req.getPhone())) {
            return "phone_" + req.getPhone();
        }
        if ("wechat".equals(loginType)) {
            return "wx_" + normalizeWechatOpenId(req.getWechatOpenId(), req.getNickname()).replaceAll("[^a-zA-Z0-9_]", "_");
        }
        if (StringUtils.hasText(req.getPhone())) {
            return "user_" + req.getPhone();
        }
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String resolvePassword(RegisterReq req, String loginType) {
        if (StringUtils.hasText(req.getPassword())) {
            return req.getPassword();
        }
        if ("phone".equals(loginType) || "wechat".equals(loginType)) {
            return UUID.randomUUID().toString();
        }
        throw new BizException(40000, "请输入密码");
    }

    private String normalizeWechatOpenId(String wechatOpenId, String nickname) {
        if (StringUtils.hasText(wechatOpenId)) {
            return wechatOpenId.trim();
        }
        if (StringUtils.hasText(nickname)) {
            return "wx_" + nickname.trim().replaceAll("\\s+", "_").toLowerCase();
        }
        throw new BizException(40000, "请提供微信身份标识");
    }

    private String inferLoginType(RegisterReq req) {
        if (StringUtils.hasText(req.getWechatOpenId())) {
            return "wechat";
        }
        if (StringUtils.hasText(req.getPhone())) {
            return "phone";
        }
        return "password";
    }

    private Map<String, String> option(String code, String label) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("label", label);
        return item;
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
            .organizationType(user.getOrganizationType())
            .loginType(user.getLoginType())
            .roleName(role == null ? null : role.getName())
            .roleCode(role == null ? null : role.getCode())
            .lastActiveAt(user.getUpdateTime() == null ? null : user.getUpdateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
            .build();
    }

    @Data
    public static class LoginReq { private String username; private String password; }

    @Data
    public static class PhoneLoginReq { private String phone; private String code; }

    @Data
    public static class WechatLoginReq {
        private String nickname;
        private String phone;
        private String wechatOpenId;
        private String roleCode;
        private String organizationType;
        private String department;
    }

    @Data
    public static class PhoneCodeReq { private String phone; }

    @Data
    public static class RegisterReq {
        private String username;
        private String password;
        private String realName;
        private String nickname;
        private String roleCode;
        private String organizationType;
        private String department;
        private String phone;
        private String phoneCode;
        private String email;
        private String loginType;
        private String wechatOpenId;
    }

    @Data
    public static class SessionResp {
        private final String token;
        private final UserProfileDTO user;
        private final boolean authenticated;
        private final long serverTime;
    }

    private record DemoAccountSeed(
        String username,
        String password,
        String realName,
        String roleCode,
        String organizationType,
        String department,
        String phone,
        String email,
        String wechatOpenId
    ) { }
}
