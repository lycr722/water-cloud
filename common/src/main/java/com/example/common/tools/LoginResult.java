package com.example.common.tools;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResult {
    private Integer flag;  //执行结果，0失败；1用户；2送水员

    private String message;  //返回结果信息
}
