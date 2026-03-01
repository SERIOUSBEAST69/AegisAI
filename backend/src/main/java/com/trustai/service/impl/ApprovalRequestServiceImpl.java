package com.trustai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trustai.entity.ApprovalRequest;
import com.trustai.mapper.ApprovalRequestMapper;
import com.trustai.service.ApprovalRequestService;
import org.springframework.stereotype.Service;

@Service
public class ApprovalRequestServiceImpl extends ServiceImpl<ApprovalRequestMapper, ApprovalRequest> implements ApprovalRequestService {
}
