package com.example.worker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.constant.WaterMessage;
import com.example.common.tools.Result;
import com.example.worker.entity.Worker;
import com.example.worker.mapper.WorkerMapper;
import com.example.worker.service.WorkerService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class WorkerServiceImpl extends ServiceImpl<WorkerMapper, Worker> implements WorkerService {
    @Resource
    private WorkerMapper workerMapper;

    @Override
    public Worker selectById(String userId) {
        return workerMapper.selectById(userId);
    }

    @Override
    @HystrixCommand(fallbackMethod = "registerFallOutMethod")
    public Result register(Worker worker) {
        Worker workerDB = workerMapper.selectById(worker.getOpenId());
        if (workerDB!=null)
        {
            throw new RuntimeException(WaterMessage.FALL_BACK_REPEAT_USER);
        }

        int flag = workerMapper.insert(worker);
        return flag==1?new Result(true, WaterMessage.REGISTER_SUCCESS,200)
                :new Result(false,WaterMessage.REGISTER_FAILD,500);
    }

    public Result registerFallOutMethod(Worker worker)
    {
        return new Result(false, WaterMessage.FALL_BACK_REPEAT_USER,500);
    }

    @Override
    public Integer updateWorker(Worker worker) {
        return workerMapper.updateById(worker);
    }

    @Override
    public Integer checkUser(String userId, String password) {
        Worker user = workerMapper.selectById(userId);
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
