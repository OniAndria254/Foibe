package org.fam.dime.importer;

import java.util.List;

public record CsvRowError(int lineNumber, String rawLine, String code, List<Object> args) {
}
