package org.fam.dime.importer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "import_row_errors")
@Getter
@Setter
public class ImportRowError {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "batch_id", nullable = false)
	private Long batchId;

	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	@Column(name = "raw_line", length = 1000)
	private String rawLine;

	@Column(name = "error_message", nullable = false, length = 500)
	private String errorMessage;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
