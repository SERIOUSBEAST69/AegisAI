package com.trustai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trustai.entity.DataShareRequest;
import com.trustai.mapper.DataShareRequestMapper;
import com.trustai.service.DataShareRequestService;
import org.springframework.stereotype.Service;

@Service
public class DataShareRequestServiceImpl extends ServiceImpl<DataShareRequestMapper, DataShareRequest> implements DataShareRequestService {
}
