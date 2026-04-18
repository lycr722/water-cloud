package com.example.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @TableId
    private String openId;

    @TableField("user_name")
    private String userName;

    private String dormi;

    private String phone;

    private String building;

    private String password;

    @TableField("avatar_url")
    private String avatarUrl;

    public User(String openId) {
        this.openId = openId;
    }
}
