package com.example.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.constant.WaterMessage;
import com.example.common.tools.Result;
import com.example.user.dto.UserDto;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserService;
import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;


@Service
@RefreshScope
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

//    @Value("${wx.appId}")
//    private String appId;
//    @Value("${wx.secret}")
//    private String secret;

    @Override
    public User selectById(String userId) {
        return userMapper.selectById(userId);
    }

    // TODO 用户名存在,抛异常进行服务降级
    @Override
    @HystrixCommand(fallbackMethod = "registerFallOutMethod")
    public Result register(User user) {
        User userDB = userMapper.selectById(user.getOpenId());
        if (userDB!=null)
        {
            throw new RuntimeException(WaterMessage.FALL_BACK_REPEAT_USER);
        }
        int flag = userMapper.insert(user);
        return flag == 1 ? new Result(true, WaterMessage.REGISTER_SUCCESS)
                : new Result(false, WaterMessage.REGISTER_FAILD);
    }
    public Result registerFallOutMethod(User user)
    {
        return new Result(false, WaterMessage.FALL_BACK_REPEAT_USER,500);
    }

    @Override
    public Integer updateUser(User user) {
        return userMapper.updateById(user);
    }

    public boolean verify(UserDto userDto){
        if (StringUtils.isBlank(userDto.getToken())){
            return false;
        }
        String token = userDto.getToken();
        String exist = stringRedisTemplate.opsForValue().get("user:" + token);
        boolean flag = "exist".equals(exist);
        // 刷新token
        if (flag){
            stringRedisTemplate.opsForValue().set("user:" + token, "exist", 72L, TimeUnit.HOURS);
        }
        System.out.println(flag);
        return flag;
    }

    @Override
    public UserDto getUserOpenId(String code) {
//        String msg = appId + ":::::::" +secret;
        return null;
    }

    @Override
    public Integer checkUser(String userId, String password) {
        User user = userMapper.selectById(userId);
        if (user==null)
        {
            return 0;
        }
        if (!password.equals(user.getPassword()))
        {
            return 1;
        }
        return 2;
    }

}
