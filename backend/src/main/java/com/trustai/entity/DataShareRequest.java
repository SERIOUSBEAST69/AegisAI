package com.trustai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("data_share_request")
public class DataShareRequest {
    @TableId
    private Long id;
    private Long assetId;
    private Long applicantId;
    private String collaborators; // comma separated
    private String reason;
    private String status; // pending/approved/rejected
    private Long approverId;
    private String shareToken;
    private Date expireTime;
    private Date createTime;
    private Date updateTime;
}
