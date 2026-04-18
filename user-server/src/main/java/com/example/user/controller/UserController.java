package com.example.user.controller;

import com.example.user.entity.User;
import com.example.common.constant.WaterMessage;
import com.example.user.service.UserService;
import com.example.common.tools.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/select")
    @Operation(summary = "通过用户id查询用户信息")
    public Result selectById(@RequestParam("userId") String userId) {
        User user = userService.selectById(userId);
        return user != null ? new Result(true,200, user)
                : new Result(false,200);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册,返回消息提示")
    public Result register(@RequestBody User user) {
        return userService.register(user);
    }

    @PutMapping("/update")
    @Operation(summary = "用户更改个人信息")
    public Result updateUser(@RequestBody User user) {
        Integer flag = userService.updateUser(user);
        return flag == 1 ? new Result(true, WaterMessage.UPDATE_SUCCESS)
                : new Result(false, WaterMessage.UPDATE_FAILD);
    }

    @Operation(summary = "根据前端的code获取用户openId，并判断是否第一次登录及身份" +
            "0:用户，1：送水员，2：新用户")
    @GetMapping("/getOpenId")
    public Result getWxOpenId(@RequestParam("code") String code){
        return  new Result(true, "登录成功!", userService.getUserOpenId(code));
    }

    @Operation(summary = "检验密码")
    @GetMapping("/check")
    public Result checkUser(@RequestParam("userId") String userId,
                            @RequestParam("password") String password){
        Integer flag = userService.checkUser(userId, password);
        if (flag==0)
        {
            User user=new User();
            user.setOpenId(userId);
            user.setPassword(password);
            Result register = userService.register(user);
            return new Result<>(false,WaterMessage.LOGIN_FAILD_NOTUSER);
        }
        if (flag==1)
        {
            return new Result<>(false,WaterMessage.LOGIN_FAILD_PASSWORD);
        }
        return new Result<>(true,WaterMessage.LOGIN_NEW);
    }
}
