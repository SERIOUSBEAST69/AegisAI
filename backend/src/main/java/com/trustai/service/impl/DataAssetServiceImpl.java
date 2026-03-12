package com.trustai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustai.dto.AiCallBriefDto;
import com.trustai.dto.DataAssetDetailDto;
import com.trustai.dto.DataAssetDto;
import com.trustai.dto.SensitiveScanReport;
import com.trustai.document.AssetDocument;
import com.trustai.entity.AiCallLog;
import com.trustai.entity.DataAsset;
import com.trustai.entity.RiskEvent;
import com.trustai.entity.SensitiveScanTask;
import com.trustai.exception.BizException;
import com.trustai.mapper.AiCallLogMapper;
import com.trustai.mapper.DataAssetMapper;
import com.trustai.repository.AssetEsRepository;
import com.trustai.service.DataAssetService;
import com.trustai.service.RiskEventService;
import com.trustai.service.SensitiveScanEngine;
import com.trustai.service.SensitiveScanTaskService;
import com.trustai.utils.AssetContentExtractor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataAssetServiceImpl extends ServiceImpl<DataAssetMapper, DataAsset> implements DataAssetService {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final AiCallLogMapper aiCallLogMapper;
	private final SensitiveScanTaskService sensitiveScanTaskService;
	private final RiskEventService riskEventService;
	private final AssetEsRepository assetEsRepository;
	private final SensitiveScanEngine sensitiveScanEngine;
	private final AssetContentExtractor assetContentExtractor;

	public DataAssetServiceImpl(AiCallLogMapper aiCallLogMapper,
							 SensitiveScanTaskService sensitiveScanTaskService,
							 RiskEventService riskEventService,
							 AssetEsRepository assetEsRepository,
							 SensitiveScanEngine sensitiveScanEngine,
							 AssetContentExtractor assetContentExtractor) {
		this.aiCallLogMapper = aiCallLogMapper;
		this.sensitiveScanTaskService = sensitiveScanTaskService;
		this.riskEventService = riskEventService;
		this.assetEsRepository = assetEsRepository;
		this.sensitiveScanEngine = sensitiveScanEngine;
		this.assetContentExtractor = assetContentExtractor;
	}

	@Override
	public DataAsset register(DataAsset entity) {
		Date now = new Date();
		entity.setCreateTime(now);
		this.save(entity);
		saveEs(entity);

		SensitiveScanTask task = new SensitiveScanTask();
		task.setAssetId(entity.getId());
		task.setSourceType(entity.getType() == null ? "asset" : entity.getType());
		task.setSourcePath(entity.getLocation() == null ? ("asset-" + entity.getId()) : entity.getLocation());
		task.setStatus("running");
		task.setCreateTime(now);
		sensitiveScanTaskService.save(task);

		runScanAndRecord(task, entity);
		return entity;
	}

	@Override
	public Page<DataAssetDto> page(Integer current, Integer size, String keyword) {
		Page<DataAsset> entityPage = new Page<>(current, size);
		LambdaQueryWrapper<DataAsset> qw = new LambdaQueryWrapper<>();
		if (keyword != null && !keyword.isEmpty()) {
			qw.like(DataAsset::getName, keyword).or().like(DataAsset::getDescription, keyword);
		}
		this.page(entityPage, qw.orderByDesc(DataAsset::getCreateTime));
		Page<DataAssetDto> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
		dtoPage.setRecords(entityPage.getRecords().stream().map(item -> {
			DataAssetDto dto = new DataAssetDto();
			BeanUtils.copyProperties(item, dto);
			return dto;
		}).collect(Collectors.toList()));
		return dtoPage;
	}

	@Override
	public DataAssetDetailDto detailWithCalls(Long id) {
		DataAsset asset = this.getById(id);
		if (asset == null) throw new BizException(40400, "数据资产不存在");
		List<AiCallBriefDto> calls = aiCallLogMapper.selectList(new LambdaQueryWrapper<AiCallLog>()
						.eq(AiCallLog::getDataAssetId, id)
						.orderByDesc(AiCallLog::getCreateTime)
						.last("limit 50"))
				.stream()
				.map(log -> {
					AiCallBriefDto dto = new AiCallBriefDto();
					dto.setId(log.getId());
					dto.setModelCode(log.getModelCode());
					dto.setCreateTime(log.getCreateTime());
					dto.setDurationMs(log.getDurationMs());
					return dto;
				})
				.collect(Collectors.toList());
		DataAssetDetailDto dto = new DataAssetDetailDto();
		BeanUtils.copyProperties(asset, dto);
		dto.setCalls(calls);
		return dto;
	}

	private void saveEs(DataAsset entity) {
		try {
			AssetDocument doc = new AssetDocument();
			doc.setId(String.valueOf(entity.getId()));
			doc.setAssetId(entity.getId());
			doc.setName(entity.getName());
			doc.setType(entity.getType());
			doc.setSensitivityLevel(entity.getSensitivityLevel());
			doc.setLocation(entity.getLocation());
			doc.setDescription(entity.getDescription());
			doc.setCreateTime(entity.getCreateTime());
			assetEsRepository.save(doc);
		} catch (Exception ignored) { }
	}

	private void runScanAndRecord(SensitiveScanTask task, DataAsset asset) {
		List<String> samples = new java.util.ArrayList<>();
		String contentPreview = assetContentExtractor.extractPreview(asset.getLocation());
		if (StringUtils.hasText(contentPreview)) {
			samples.add(contentPreview);
		}
		if (asset.getDescription() != null && !asset.getDescription().isEmpty()) {
			samples.add(asset.getDescription());
		}
		if (!StringUtils.hasText(contentPreview) && asset.getLocation() != null && !asset.getLocation().isEmpty()) {
			samples.add(asset.getLocation());
		}
		if (samples.isEmpty() && asset.getName() != null) {
			samples.add(asset.getName());
		}

		SensitiveScanReport report = sensitiveScanEngine.scan(samples);
		double ratio = report.getSummary() == null ? 0.0 : report.getSummary().getRatio();

		try {
			String json = MAPPER.writeValueAsString(report);
			task.setReportData(json);
		} catch (Exception ignored) { }
		task.setSensitiveRatio(ratio);
		task.setStatus("done");
		task.setReportPath("/reports/task-" + task.getId() + ".json");
		task.setUpdateTime(Date.from(Instant.now()));
		sensitiveScanTaskService.updateById(task);

		String level = ratio > 60 ? "critical" : (ratio > 30 ? "high" : "medium");
		RiskEvent event = new RiskEvent();
		event.setType("敏感数据扫描");
		event.setLevel(level);
		event.setRelatedLogId(task.getId());
		event.setStatus("open");
		event.setProcessLog("自动创建，敏感占比" + String.format("%.2f", ratio) + "%");
		event.setCreateTime(new Date());
		event.setUpdateTime(new Date());
		riskEventService.save(event);
	}
}
