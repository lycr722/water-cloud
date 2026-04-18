package com.example.feign.clients;

import com.example.common.tools.Result;
import com.example.feign.clients.impl.UserClientFallback;
import com.example.feign.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(value = "user-server",fallback = UserClientFallback.class)
public interface UserClient {
    @GetMapping("/user/select")
    Result<User> selectById(@RequestParam("userId") String userId);
}
