package org.fam.dime.audit.dto;

import java.time.LocalDate;
import org.fam.dime.audit.AuditAction;
import org.fam.dime.audit.AuditEntityType;
import org.springframework.format.annotation.DateTimeFormat;

public record AuditFilter(
		AuditEntityType entityType,
		AuditAction action,
		String username,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
}