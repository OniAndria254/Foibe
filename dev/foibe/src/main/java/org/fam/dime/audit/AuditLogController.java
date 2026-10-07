package org.fam.dime.audit;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.fam.dime.audit.dto.AuditFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/audit")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	public String list(
			@ModelAttribute("filter") AuditFilter filter,
			@PageableDefault(size = 25, sort = "changedAt", direction = Sort.Direction.DESC) Pageable pageable,
			Model model) {
		model.addAttribute("results", auditLogService.search(
				filter.entityType(),
				filter.action(),
				filter.username(),
				toInstant(filter.from()),
				toInstantExclusive(filter.to()),
				pageable));
		model.addAttribute("entityTypes", AuditEntityType.values());
		model.addAttribute("actions", AuditAction.values());
		return "audit/list";
	}

	private Instant toInstant(LocalDate date) {
		return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	private Instant toInstantExclusive(LocalDate date) {
		return date == null ? null : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
	}
}