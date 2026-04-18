package com.example.feign.clients;

import com.example.common.tools.Result;
import com.example.feign.clients.impl.UserClientFallback;
import com.example.feign.entity.User;
import com.example.feign.entity.Worker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(value = "worker-server")
public interface WorkerClient {
    @GetMapping("/worker/select")
    Result<Worker> selectById(@RequestParam("workerId") String workerId);
}
