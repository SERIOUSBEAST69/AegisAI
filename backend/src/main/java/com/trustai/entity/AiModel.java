package com.trustai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("ai_model")
public class AiModel {
    @TableId
    private Long id;
    private String name;
    private String type;
    private String riskLevel;
    private String endpoint;
    private String developer;
    private Integer status;
    private String description;
    private Date createTime;
    private Date updateTime;
}
