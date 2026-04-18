package com.example.worker.controller;

import com.example.worker.entity.Worker;
import com.example.common.constant.WaterMessage;
import com.example.worker.service.WorkerService;
import com.example.common.tools.Result;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/worker")
public class WorkerController {
    @Resource
    private WorkerService workerService;

    @GetMapping("/select")
    @Operation(summary = "通过送水员id查询送水员信息")
    public Result selectById(@RequestParam("workerId") String workerId){
        Worker worker = workerService.selectById(workerId);
        return worker!=null?new Result(true,worker)
                :new Result(false);
    }

    @PostMapping("/register")
    @Operation(summary = "送水员注册,返回消息提示")
    public Result register(@RequestBody Worker worker){
        return workerService.register(worker);
    }

    @PutMapping("/update")
    @Operation(summary = "送水员更改个人信息")
    public Result updateWorker(@RequestBody Worker worker){
        Integer flag = workerService.updateWorker(worker);
        return flag==1?new Result(true, WaterMessage.UPDATE_SUCCESS)
                :new Result(false,WaterMessage.UPDATE_FAILD);
    }

    @Operation(summary = "检验密码")
    @GetMapping("/check")
    public Result checkUser(@RequestParam("userId") String userId,
                            @RequestParam("password") String password){
        Integer flag = workerService.checkUser(userId, password);
        if (flag==0)
        {
            Worker user=new Worker();
            user.setOpenId(userId);
            user.setPassword(password);
            Result register = workerService.register(user);
            return new Result<>(false,WaterMessage.LOGIN_FAILD_NOTUSER);
        }
        if (flag==1)
        {
            return new Result<>(false,WaterMessage.LOGIN_FAILD_PASSWORD);
        }
        return new Result<>(true,WaterMessage.LOGIN_WORKER);
    }
}
