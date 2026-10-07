package org.fam.dime.audit;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository
		extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

	List<AuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(AuditEntityType entityType, Long entityId);

	default Page<AuditLog> search(
			AuditEntityType entityType,
			AuditAction action,
			Long changedBy,
			Instant from,
			Instant to,
			Pageable pageable) {
		return findAll(
				Specification.allOf(
						eq("entityType", entityType),
						eq("action", action),
						eq("changedBy", changedBy),
						between("changedAt", from, to)),
				pageable);
	}

	private static Specification<AuditLog> eq(String field, Object value) {
		return (root, query, cb) -> value == null ? cb.conjunction() : cb.equal(root.get(field), value);
	}

	private static Specification<AuditLog> between(String field, Instant from, Instant to) {
		return (root, query, cb) -> {
			if (from == null && to == null) {
				return cb.conjunction();
			}
			var path = root.<Instant>get(field);
			if (from != null && to != null) {
				return cb.between(path, from, to);
			}
			return from != null ? cb.greaterThanOrEqualTo(path, from) : cb.lessThanOrEqualTo(path, to);
		};
	}
}