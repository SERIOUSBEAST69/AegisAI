package com.trustai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trustai.entity.SensitiveScanTask;
import com.trustai.mapper.SensitiveScanTaskMapper;
import com.trustai.service.SensitiveScanTaskService;
import org.springframework.stereotype.Service;

@Service
public class SensitiveScanTaskServiceImpl extends ServiceImpl<SensitiveScanTaskMapper, SensitiveScanTask> implements SensitiveScanTaskService {
}
