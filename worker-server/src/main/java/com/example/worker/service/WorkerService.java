package com.example.worker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.tools.Result;
import com.example.worker.entity.Worker;


public interface WorkerService extends IService<Worker> {
    Worker selectById(String userId);

    Result register(Worker worker);

    Integer updateWorker(Worker worker);

    Integer checkUser(String userId,String password);
}
