package com.example.feign.clients;

import com.example.common.tools.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient("order-server")
public interface OrderClient {
    @GetMapping("/order/quantity")
    Result selectQuantity(@RequestParam("workerId") String workerId,
                          @RequestParam(name = "building", required = false) String building,
                          @RequestParam(name = "delivery", required = false) Integer delivery);
}
