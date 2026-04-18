package com.example.worker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.worker.entity.Worker;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkerMapper extends BaseMapper<Worker> {
}
