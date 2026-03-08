package com.trustai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_user")
public class User {
    @TableId
    private Long id;
    private String username;
    private String password;
    private String realName;
    private Long roleId;
    private String department;
    private String phone;
    private String email;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
