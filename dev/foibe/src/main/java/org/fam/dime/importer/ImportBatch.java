package org.fam.dime.importer;

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

@Entity
@Table(name = "import_batches")
@Getter
@Setter
public class ImportBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "church_id", nullable = false)
	private Long churchId;

	@Column(name = "filename", nullable = false, length = 255)
	private String filename;

	@Column(name = "source_text", nullable = false, length = 500000)
	private String sourceText;

	@Column(name = "status", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ImportBatchStatus status;

	@Column(name = "total_rows", nullable = false)
	private int totalRows;

	@Column(name = "valid_rows", nullable = false)
	private int validRows;

	@Column(name = "error_rows", nullable = false)
	private int errorRows;

	@Column(name = "assigned_from")
	private Integer assignedFrom;

	@Column(name = "assigned_to")
	private Integer assignedTo;

	@Column(name = "ignored_columns", length = 500)
	private String ignoredColumns;

	@Column(name = "preview_members", columnDefinition = "text")
	private String previewMembers;

	@Column(name = "started_by")
	private Long startedBy;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "applied_at")
	private Instant appliedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_by")
	private Long updatedBy;
}
