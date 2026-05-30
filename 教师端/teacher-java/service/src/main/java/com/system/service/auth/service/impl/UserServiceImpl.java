package com.system.service.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.system.dto.SysLoginUserInfoDTO;
import com.system.dto.SysUserDTO;
import com.system.service.auth.dto.LoginRequestDTO;
import com.system.service.auth.entity.User;
import com.system.service.auth.mapper.UserMapper;
import com.system.service.auth.service.UserService;
import com.system.service.auth.vo.UserInfoVO;
import com.system.service.profile.entity.TeacherProfile;
import com.system.service.profile.service.TeacherProfileService;
import com.system.utils.LoginUtil;
import com.system.web.MyException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private TeacherProfileService teacherProfileService;

    @Override
    public String login(LoginRequestDTO loginDTO) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        String turnstileToken = loginDTO.getTurnstileToken();

        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new MyException("账号或密码不能为空");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username).eq(User::getActive, 1);
        User user = this.getOne(queryWrapper);

        if (user == null) {
            throw new MyException("账号或密码错误");
        }

        String salt = user.getPasswordSalt() == null ? "" : user.getPasswordSalt();
        String inputHash = SecureUtil.sha256(password + salt);
        
        if (!inputHash.equalsIgnoreCase(user.getPasswordHash())) {
            throw new MyException("账号或密码错误");
        }


        if (StrUtil.isBlank(turnstileToken)) {
            throw new MyException("NEED_CAPTCHA");
        }

        verifyTurnstile(turnstileToken);

        if (!"TEACHER".equalsIgnoreCase(user.getRole())) {
            throw new MyException("该账号并非老师账号，无权登录");
        }

        StpUtil.login(user.getId());

        SysLoginUserInfoDTO userInfoDTO = new SysLoginUserInfoDTO();
        SysUserDTO userDTO = new SysUserDTO();
        userDTO.setId(user.getId().intValue());
        userDTO.setName(user.getUsername());

        TeacherProfile profile = teacherProfileService.getByUserId(user.getId());
        if (profile != null) {
            userDTO.setAvatar(profile.getAvatar());
        }
        
        userDTO.setEnabled(user.getActive() == 1);
        userDTO.setAutoRenewal(true);
        userDTO.setIsDemo(false); 
        userInfoDTO.setUser(userDTO);

        StpUtil.getSession().set(LoginUtil.SYS_USER_KEY, userInfoDTO);
        StpUtil.getSession().set("Role", user.getRole());

        return StpUtil.getTokenValue();
    }

    @Override
    public UserInfoVO getUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = this.getById(userId);
        if (user == null) return null;

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setActive(user.getActive());

        TeacherProfile profile = teacherProfileService.getByUserId(userId);
        if (profile != null) {
            vo.setName(profile.getName());
            vo.setAvatar(profile.getAvatar());
            vo.setEmail(profile.getEmail());
        } else {
            vo.setName(user.getUsername());
        }

        return vo;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @org.springframework.beans.factory.annotation.Value("${cloudflare.turnstile.secret:1x0000000000000000000000000000000AA}")
    private String turnstileSecret;

    private void verifyTurnstile(String token) {
        if (StrUtil.isBlank(token)) {
            throw new MyException("请完成安全验证");
        }

        try {
            String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
            Map<String, Object> params = new HashMap<>();
            params.put("secret", turnstileSecret);
            params.put("response", token);

            String body = HttpUtil.post(url, params);
            JSONObject json = JSONUtil.parseObj(body);

            if (!Boolean.TRUE.equals(json.getBool("success"))) {
                throw new MyException("安全验证失败，请重试");
            }
        } catch (Exception e) {
            throw new MyException("安全验证服务连接失败，请检查网络是否能访问 Cloudflare");
        }
    }
}
