package org.fam.dime.importer.dto;

import java.time.Instant;
import java.util.List;
import org.fam.dime.importer.ImportBatchStatus;

public record ImportReportResponse(
		Long id,
		String filename,
		ImportBatchStatus status,
		int totalRows,
		int validRows,
		int errorRows,
		Integer assignedFrom,
		Integer assignedTo,
		List<String> ignoredColumns,
		String startedBy,
		Instant startedAt,
		Instant appliedAt,
		List<ImportRowErrorResponse> rowErrors,
		List<ImportPreviewRow> previewRows) {

	public String getStatusLabel() {
		return "import.status." + status.name();
	}
}