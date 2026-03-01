package com.trustai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trustai.entity.RiskEvent;
import com.trustai.mapper.RiskEventMapper;
import com.trustai.service.RiskEventService;
import org.springframework.stereotype.Service;

@Service
public class RiskEventServiceImpl extends ServiceImpl<RiskEventMapper, RiskEvent> implements RiskEventService {
}
