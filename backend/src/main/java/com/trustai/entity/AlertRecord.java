package com.trustai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("alert_record")
public class AlertRecord {
    @TableId
    private Long id;
    private String title;
    private String level;
    private String status; // open/claimed/resolved/archived
    private Long assigneeId;
    private Long relatedLogId;
    private String resolution;
    private Date createTime;
    private Date updateTime;
}
