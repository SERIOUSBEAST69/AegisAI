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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final RabbitTemplate rabbitTemplate;
	private final AuditLogEsRepository auditLogEsRepository;

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

	@Override
	public List<AuditLogDocument> search(Long userId, String operation, Date from, Date to) {
		String normalizedOperation = operation == null ? "" : operation.trim().toLowerCase(Locale.ROOT);

		return StreamSupport.stream(auditLogEsRepository.findAll().spliterator(), false)
				.filter(doc -> userId == null || userId.equals(doc.getUserId()))
				.filter(doc -> normalizedOperation.isEmpty()
						|| containsIgnoreCase(doc.getOperation(), normalizedOperation)
						|| containsIgnoreCase(doc.getInputOverview(), normalizedOperation)
						|| containsIgnoreCase(doc.getOutputOverview(), normalizedOperation)
						|| containsIgnoreCase(doc.getResult(), normalizedOperation))
				.filter(doc -> from == null || (doc.getOperationTime() != null && !doc.getOperationTime().before(from)))
				.filter(doc -> to == null || (doc.getOperationTime() != null && !doc.getOperationTime().after(to)))
				.collect(Collectors.toList());
	}

	private boolean containsIgnoreCase(String source, String normalizedKeyword) {
		return source != null && source.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
	}
}
