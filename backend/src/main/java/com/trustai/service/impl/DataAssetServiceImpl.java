package com.trustai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trustai.entity.DataAsset;
import com.trustai.mapper.DataAssetMapper;
import com.trustai.service.DataAssetService;
import org.springframework.stereotype.Service;

@Service
public class DataAssetServiceImpl extends ServiceImpl<DataAssetMapper, DataAsset> implements DataAssetService {
}
