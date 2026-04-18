package com.example.common.tools;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 封装返回结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {
    private boolean flag;  //执行结果，true为执行成功 false为执行失败

    private Integer code;  //返回码 200成功 非200失败
    private String message;  //返回结果信息
    private T data;  //返回数据

    public Result(boolean flag, String message) {
        super();
        this.flag = flag;
        this.message = message;
    }

    public Result(boolean flag, String message, Integer code){
        super();
        this.flag = flag;
        this.message = message;
        this.code = code;
    }

    public Result(boolean flag, String message, T data) {
        this.flag = flag;
        this.message = message;
        this.data = data;
    }

    public Result(boolean flag, T data) {
        this.flag = flag;
        this.data = data;
    }

    public Result(boolean flag) {
        this.flag = flag;
    }

    public Result(boolean flag, Integer code, T data) {
        this.flag = flag;
        this.code = code;
        this.data = data;
    }

    public Result(boolean flag, Integer code) {
        this.flag = flag;
        this.code = code;
    }
}
