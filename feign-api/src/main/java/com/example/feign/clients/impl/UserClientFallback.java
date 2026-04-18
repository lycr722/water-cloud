package com.example.feign.clients.impl;

import com.example.common.constant.WaterMessage;
import com.example.common.tools.Result;
import com.example.feign.clients.UserClient;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public Result selectById(String userId) {
        return new Result(false, WaterMessage.FALL_BACK_CALL,500);
    }
}
