package com.system.service.profile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.system.service.profile.entity.TeacherProfile;


public interface TeacherProfileService extends IService<TeacherProfile> {

    TeacherProfile getByUserId(Long userId);

    void saveOrUpdateProfile(Long userId, TeacherProfile profile);
}
