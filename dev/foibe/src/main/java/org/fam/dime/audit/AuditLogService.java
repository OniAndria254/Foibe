package org.fam.dime.audit;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.fam.dime.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;
	private final CurrentUserService currentUserService;
	private final ObjectMapper objectMapper;

	public AuditLogService(
			AuditLogRepository auditLogRepository,
			CurrentUserService currentUserService,
			ObjectMapper objectMapper) {
		this.auditLogRepository = auditLogRepository;
		this.currentUserService = currentUserService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public AuditLog recordChange(
			Long churchId,
			AuditEntityType entityType,
			Long entityId,
			AuditAction action,
			Object before,
			Object after) {

		AuditLog auditLog = new AuditLog();
		auditLog.setChurchId(churchId);
		auditLog.setEntityType(entityType);
		auditLog.setEntityId(entityId);
		auditLog.setAction(action);
		auditLog.setOldValues(toJson(before));
		auditLog.setNewValues(toJson(after));
		auditLog.setChangedBy(currentUserService.currentUserIdOrNull());
		auditLog.setChangedAt(Instant.now());
		return auditLogRepository.save(auditLog);
	}

	@Transactional(readOnly = true)
	public Page<AuditLog> search(
			AuditEntityType entityType,
			AuditAction action,
			String username,
			Instant from,
			Instant to,
			Pageable pageable) {

		Long changedBy = resolveUserId(username);
		return auditLogRepository.search(entityType, action, changedBy, from, to, pageable);
	}

	@Transactional(readOnly = true)
	public List<AuditLog> historyFor(AuditEntityType entityType, Long entityId) {
		return auditLogRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(entityType, entityId);
	}

	private Long resolveUserId(String username) {
		if (username == null || username.isBlank()) {
			return null;
		}
		return currentUserService.findUserIdByUsername(username).orElse(null);
	}

	private String toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Cannot serialise audit payload", exception);
		}
	}
}
