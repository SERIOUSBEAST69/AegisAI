package com.trustai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustai.config.RabbitConfig;
import com.trustai.document.AuditLogDocument;
import com.trustai.entity.AuditLog;
import com.trustai.mapper.AuditLogMapper;
import com.trustai.repository.AuditLogEsRepository;
import com.trustai.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	/** 单次搜索最大返回条数，防止全量扫描 */
	private static final int MAX_RESULTS = 1000;

	private final RabbitTemplate rabbitTemplate;
	private final AuditLogEsRepository auditLogEsRepository;
	private final ElasticsearchOperations elasticsearchOperations;

	@Override
	public boolean save(AuditLog entity) {
		boolean db = super.save(entity);
		sendAsync(entity);
		return db;
	}

	@Override
	public boolean saveAudit(AuditLog log) {
		boolean db = super.save(log);
		sendAsync(log);
		return db;
	}

	private void sendAsync(AuditLog log) {
		try {
			rabbitTemplate.convertAndSend(RabbitConfig.AUDIT_LOG_QUEUE, MAPPER.writeValueAsString(log));
		} catch (Exception ignored) { }
	}

	/**
	 * ES 原生查询优化：将过滤条件下推至 Elasticsearch，避免全量拉取后在内存中过滤。
	 * 使用 {@link CriteriaQuery} 构建 bool 查询，最多返回 {@value #MAX_RESULTS} 条。
	 */
	@Override
	public List<AuditLogDocument> search(Long userId, String operation, Date from, Date to) {
		String keyword = (operation == null) ? "" : operation.trim().toLowerCase(Locale.ROOT);

		Criteria criteria = null;

		if (userId != null) {
			criteria = new Criteria("userId").is(userId);
		}

		if (StringUtils.hasText(keyword)) {
			Criteria textCriteria = new Criteria("operation").contains(keyword)
					.or(new Criteria("inputOverview").contains(keyword))
					.or(new Criteria("outputOverview").contains(keyword))
					.or(new Criteria("result").contains(keyword));
			criteria = (criteria == null) ? textCriteria : criteria.and(textCriteria);
		}

		if (from != null) {
			Criteria fromCriteria = new Criteria("operationTime").greaterThanEqual(from);
			criteria = (criteria == null) ? fromCriteria : criteria.and(fromCriteria);
		}

		if (to != null) {
			Criteria toCriteria = new Criteria("operationTime").lessThanEqual(to);
			criteria = (criteria == null) ? toCriteria : criteria.and(toCriteria);
		}

		CriteriaQuery query = new CriteriaQuery(
				criteria != null ? criteria : new Criteria(),
				PageRequest.of(0, MAX_RESULTS)
		);

		try {
			return elasticsearchOperations.search(query, AuditLogDocument.class)
					.stream()
					.map(SearchHit::getContent)
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.warn("ES native query failed, falling back to findAll filter", e);
			// 降级：全量拉取后内存过滤
			return fallbackSearch(userId, keyword, from, to);
		}
	}

	private List<AuditLogDocument> fallbackSearch(Long userId, String normalizedOperation, Date from, Date to) {
		return StreamSupport.stream(auditLogEsRepository.findAll().spliterator(), false)
				.filter(doc -> userId == null || userId.equals(doc.getUserId()))
				.filter(doc -> normalizedOperation.isEmpty()
						|| containsIgnoreCase(doc.getOperation(), normalizedOperation)
						|| containsIgnoreCase(doc.getInputOverview(), normalizedOperation)
						|| containsIgnoreCase(doc.getOutputOverview(), normalizedOperation)
						|| containsIgnoreCase(doc.getResult(), normalizedOperation))
				.filter(doc -> from == null || (doc.getOperationTime() != null && !doc.getOperationTime().before(from)))
				.filter(doc -> to == null || (doc.getOperationTime() != null && !doc.getOperationTime().after(to)))
				.limit(MAX_RESULTS)
				.collect(Collectors.toList());
	}

	private boolean containsIgnoreCase(String source, String normalizedKeyword) {
		return source != null && source.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
	}
}
