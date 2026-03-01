package com.trustai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trustai.entity.AiModel;
import com.trustai.mapper.AiModelMapper;
import com.trustai.service.AiModelService;
import org.springframework.stereotype.Service;

@Service
public class AiModelServiceImpl extends ServiceImpl<AiModelMapper, AiModel> implements AiModelService {
}
