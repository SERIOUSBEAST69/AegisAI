package com.trustai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trustai.entity.AuditLog;
import com.trustai.mapper.AuditLogMapper;
import com.trustai.service.AuditLogService;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {
}
