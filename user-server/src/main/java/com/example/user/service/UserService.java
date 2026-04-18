package com.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.tools.Result;
import com.example.user.dto.UserDto;
import com.example.user.entity.User;


public interface UserService extends IService<User> {
    User selectById(String userId);

    Result register(User user);

    Integer updateUser(User user);

    public UserDto getUserOpenId(String code);

    Integer checkUser(String userId,String password);
}
