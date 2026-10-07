package org.fam.dime.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "church_id")
	private Long churchId;

	@Column(name = "entity_type", nullable = false, length = 60)
	@Enumerated(EnumType.STRING)
	private AuditEntityType entityType;

	@Column(name = "entity_id", nullable = false)
	private Long entityId;

	@Column(name = "action", nullable = false, length = 30)
	@Enumerated(EnumType.STRING)
	private AuditAction action;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "old_values")
	private String oldValues;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "new_values")
	private String newValues;

	@Column(name = "changed_by")
	private Long changedBy;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private Instant changedAt;
}
