package org.fam.dime.importer;

import java.util.List;

public record CsvParseResult(
		List<String> ignoredColumns,
		List<ParsedMemberRow> validRows,
		List<CsvRowError> rowErrors,
		int totalRows) {
}
