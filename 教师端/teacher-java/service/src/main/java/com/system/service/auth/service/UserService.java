package com.system.service.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.system.service.auth.dto.LoginRequestDTO;
import com.system.service.auth.entity.User;
import com.system.service.auth.vo.UserInfoVO;

public interface UserService extends IService<User> {

    String login(LoginRequestDTO loginDTO);

    UserInfoVO getUserInfo();

    void logout();
}
