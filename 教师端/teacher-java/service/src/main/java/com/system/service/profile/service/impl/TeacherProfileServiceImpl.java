package com.system.service.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.system.service.profile.entity.TeacherProfile;
import com.system.service.profile.mapper.TeacherProfileMapper;
import com.system.service.profile.service.TeacherProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 教师档案业务层实现
 */
@Slf4j
@Service
public class TeacherProfileServiceImpl extends ServiceImpl<TeacherProfileMapper, TeacherProfile> implements TeacherProfileService {

    @Override
    public TeacherProfile getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getTeacherId, userId));
    }

    @Override
    public void saveOrUpdateProfile(Long userId, TeacherProfile profile) {
        TeacherProfile existing = getByUserId(userId);
        profile.setTeacherId(userId);
        if (existing != null) {
            profile.setId(existing.getId());
            updateById(profile);
        } else {
            save(profile);
        }

        if (profile.getAvatar() != null) {
            com.system.dto.SysLoginUserInfoDTO sessionInfo = com.system.utils.LoginUtil.getSysUserInfo();
            if (sessionInfo != null && sessionInfo.getUser() != null) {
                sessionInfo.getUser().setAvatar(profile.getAvatar());
                cn.dev33.satoken.stp.StpUtil.getSession().set(com.system.utils.LoginUtil.SYS_USER_KEY, sessionInfo);
            }
        }
    }
}
