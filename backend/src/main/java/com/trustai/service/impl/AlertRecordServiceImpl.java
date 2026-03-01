package com.trustai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trustai.entity.AlertRecord;
import com.trustai.mapper.AlertRecordMapper;
import com.trustai.service.AlertRecordService;
import org.springframework.stereotype.Service;

@Service
public class AlertRecordServiceImpl extends ServiceImpl<AlertRecordMapper, AlertRecord> implements AlertRecordService {
}
