package org.fam.dime.importer;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.fam.dime.member.MemberStatus;
import org.fam.dime.member.Sex;

public final class MemberCsvParser {

	public static final String FIELD_LAST_NAME = "last_name";
	public static final String FIELD_FIRST_NAME = "first_name";
	public static final String FIELD_SEX = "sex";
	public static final String FIELD_BIRTH_DATE = "birth_date";
	public static final String FIELD_PHONE = "phone";
	public static final String FIELD_ADDRESS = "address";
	public static final String FIELD_STATUS = "status";
	public static final String FIELD_JOINED_ON = "joined_on";
	public static final String FIELD_NOTES = "notes";
	public static final String FIELD_ENVELOPE_NUMBER = "envelope_number";

	private static final Set<String> REQUIRED_FIELDS = Set.of(FIELD_LAST_NAME, FIELD_FIRST_NAME);

	private static final Set<String> USED_FIELDS = Set.of(
			FIELD_LAST_NAME, FIELD_FIRST_NAME, FIELD_SEX, FIELD_BIRTH_DATE,
			FIELD_PHONE, FIELD_ADDRESS, FIELD_STATUS, FIELD_JOINED_ON, FIELD_NOTES);

	private static final char[] SEPARATOR_CANDIDATES = { ';', ',', '\t', '|' };

	private static final DateTimeFormatter[] DATE_FORMATS = {
			DateTimeFormatter.ISO_LOCAL_DATE,
			DateTimeFormatter.ofPattern("dd/MM/uuuu"),
			DateTimeFormatter.ofPattern("d/M/uuuu")
	};

	private MemberCsvParser() {
	}

	public static String decodeUtf8(byte[] content) {
		try {
			return StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(content))
					.toString();
		} catch (CharacterCodingException exception) {
			throw new ImportFileException("IMPORT_FILE_NOT_UTF8");
		}
	}

	public static CsvParseResult parse(String content) {
		if (content == null || content.isBlank()) {
			throw new ImportFileException("IMPORT_EMPTY_FILE");
		}

		char separator = detectSeparator(content);
		List<CsvRecord> records = readRecords(content, separator);
		if (records.size() < 2) {
			throw new ImportFileException("IMPORT_EMPTY_FILE");
		}

		HeaderMapping header = mapHeader(records.get(0));
		List<ParsedMemberRow> validRows = new ArrayList<>();
		List<CsvRowError> rowErrors = new ArrayList<>();
		int totalRows = 0;

		for (int index = 1; index < records.size(); index++) {
			CsvRecord record = records.get(index);
			totalRows++;
			Map<String, String> values = extractValues(record, header.columns());
			ParsedMemberRow row = validate(record, values, rowErrors);
			if (row != null) {
				validRows.add(row);
			}
		}

		Set<String> ignoredColumns = new LinkedHashSet<>(header.ignoredFields());
		for (String field : header.columns().keySet()) {
			if (!USED_FIELDS.contains(field)) {
				ignoredColumns.add(field);
			}
		}

		return new CsvParseResult(List.copyOf(ignoredColumns), validRows, rowErrors, totalRows);
	}

	private static ParsedMemberRow validate(
			CsvRecord record,
			Map<String, String> values,
			List<CsvRowError> rowErrors) {

		String lastName = value(values, FIELD_LAST_NAME);
		String firstName = value(values, FIELD_FIRST_NAME);

		if (lastName.isEmpty()) {
			return reject(record, rowErrors, "IMPORT_ROW_MISSING_LAST_NAME");
		}
		if (firstName.isEmpty()) {
			return reject(record, rowErrors, "IMPORT_ROW_MISSING_FIRST_NAME");
		}
		if (lastName.length() > 100 || firstName.length() > 100) {
			return reject(record, rowErrors, "IMPORT_ROW_NAME_TOO_LONG");
		}

		String sexValue = value(values, FIELD_SEX);
		Sex sex = null;
		if (!sexValue.isEmpty()) {
			sex = parseSex(sexValue);
			if (sex == null) {
				return reject(record, rowErrors, "IMPORT_ROW_INVALID_SEX", sexValue);
			}
		}

		String birthDateValue = value(values, FIELD_BIRTH_DATE);
		LocalDate birthDate = null;
		if (!birthDateValue.isEmpty()) {
			birthDate = parseDate(birthDateValue);
			if (birthDate == null) {
				return reject(record, rowErrors, "IMPORT_ROW_INVALID_BIRTH_DATE", birthDateValue);
			}
		}

		String statusValue = value(values, FIELD_STATUS);
		MemberStatus status = MemberStatus.ACTIVE;
		if (!statusValue.isEmpty()) {
			status = parseStatus(statusValue);
			if (status == null) {
				return reject(record, rowErrors, "IMPORT_ROW_INVALID_STATUS", statusValue);
			}
		}

		String joinedOnValue = value(values, FIELD_JOINED_ON);
		LocalDate joinedOn = null;
		if (!joinedOnValue.isEmpty()) {
			joinedOn = parseDate(joinedOnValue);
			if (joinedOn == null) {
				return reject(record, rowErrors, "IMPORT_ROW_INVALID_JOINED_ON", joinedOnValue);
			}
		}

		String phone = value(values, FIELD_PHONE);
		if (phone.length() > 30) {
			return reject(record, rowErrors, "IMPORT_ROW_PHONE_TOO_LONG");
		}

		String address = value(values, FIELD_ADDRESS);
		if (address.length() > 300) {
			return reject(record, rowErrors, "IMPORT_ROW_ADDRESS_TOO_LONG");
		}

		String notes = value(values, FIELD_NOTES);
		if (notes.length() > 2000) {
			return reject(record, rowErrors, "IMPORT_ROW_NOTES_TOO_LONG");
		}

		return new ParsedMemberRow(
				record.lineNumber(),
				truncate(record.raw()),
				lastName,
				firstName,
				sex,
				birthDate,
				emptyToNull(phone),
				emptyToNull(address),
				status,
				joinedOn,
				emptyToNull(notes));
	}

	private static ParsedMemberRow reject(
			CsvRecord record,
			List<CsvRowError> rowErrors,
			String code,
			Object... args) {
		rowErrors.add(new CsvRowError(record.lineNumber(), truncate(record.raw()), code, List.of(args)));
		return null;
	}

	private static String value(Map<String, String> values, String field) {
		String value = values.get(field);
		return value == null ? "" : value;
	}

	private static String emptyToNull(String value) {
		return value == null || value.isEmpty() ? null : value;
	}

	private static char detectSeparator(String content) {
		int[] counts = new int[SEPARATOR_CANDIDATES.length];
		boolean inQuotes = false;

		for (int index = 0; index < content.length(); index++) {
			char current = content.charAt(index);
			if (current == '"') {
				inQuotes = !inQuotes;
				continue;
			}
			if (!inQuotes && (current == '\n' || current == '\r')) {
				break;
			}
			if (inQuotes) {
				continue;
			}
			for (int candidate = 0; candidate < SEPARATOR_CANDIDATES.length; candidate++) {
				if (current == SEPARATOR_CANDIDATES[candidate]) {
					counts[candidate]++;
				}
			}
		}

		int best = 0;
		for (int candidate = 1; candidate < counts.length; candidate++) {
			if (counts[candidate] > counts[best]) {
				best = candidate;
			}
		}
		if (counts[best] == 0) {
			throw new ImportFileException("IMPORT_SEPARATOR_NOT_FOUND");
		}
		return SEPARATOR_CANDIDATES[best];
	}

	private static HeaderMapping mapHeader(CsvRecord header) {
		Map<String, String> aliases = headerAliases();
		Map<String, Integer> columns = new HashMap<>();
		Set<String> ignored = new LinkedHashSet<>();
		Set<String> missing = new LinkedHashSet<>(REQUIRED_FIELDS);

		for (int index = 0; index < header.cells().size(); index++) {
			String normalised = normaliseHeader(header.cells().get(index));
			String canonical = aliases.get(normalised);
			if (canonical == null) {
				if (!normalised.isEmpty()) {
					ignored.add(header.cells().get(index).trim());
				}
				continue;
			}
			if (columns.putIfAbsent(canonical, index) == null) {
				missing.remove(canonical);
			}
		}

		if (!missing.isEmpty()) {
			throw new ImportFileException("IMPORT_MISSING_COLUMNS", String.join(", ", missing));
		}
		return new HeaderMapping(columns, ignored);
	}

	private static Map<String, String> extractValues(CsvRecord record, Map<String, Integer> columns) {
		Map<String, String> values = new HashMap<>();
		columns.forEach((field, index) -> values.put(field, cellAt(record, index)));
		return values;
	}

	private static String cellAt(CsvRecord record, int index) {
		List<String> cells = record.cells();
		if (index < 0 || index >= cells.size()) {
			return "";
		}
		return cells.get(index).trim();
	}

	private static List<CsvRecord> readRecords(String content, char separator) {
		List<CsvRecord> records = new ArrayList<>();
		List<String> cells = new ArrayList<>();
		StringBuilder cell = new StringBuilder();
		StringBuilder raw = new StringBuilder();
		boolean inQuotes = false;
		boolean atCellStart = true;
		int line = 1;
		int recordStartLine = 1;

		for (int index = 0; index < content.length(); index++) {
			char current = content.charAt(index);

			if (inQuotes) {
				raw.append(current);
				if (current == '"') {
					if (index + 1 < content.length() && content.charAt(index + 1) == '"') {
						cell.append('"');
						raw.append('"');
						index++;
					} else {
						inQuotes = false;
					}
				} else {
					cell.append(current);
					if (current == '\n') {
						line++;
					}
				}
				continue;
			}

			if (current == '"' && atCellStart) {
				inQuotes = true;
				raw.append(current);
				atCellStart = false;
				continue;
			}

			if (current == separator) {
				cells.add(cell.toString());
				cell.setLength(0);
				raw.append(current);
				atCellStart = true;
				continue;
			}

			if (current == '\r' || current == '\n') {
				cells.add(cell.toString());
				cell.setLength(0);
				addRecord(records, recordStartLine, raw.toString(), cells);
				cells.clear();
				raw.setLength(0);
				if (current == '\r' && index + 1 < content.length() && content.charAt(index + 1) == '\n') {
					index++;
				}
				line++;
				recordStartLine = line;
				atCellStart = true;
				continue;
			}

			cell.append(current);
			raw.append(current);
			atCellStart = false;
		}

		cells.add(cell.toString());
		addRecord(records, recordStartLine, raw.toString(), cells);
		return records;
	}

	private static void addRecord(List<CsvRecord> records, int lineNumber, String raw, List<String> cells) {
		boolean blank = cells.size() == 1 && cells.get(0).isBlank();
		if (blank) {
			return;
		}
		records.add(new CsvRecord(lineNumber, raw, List.copyOf(cells)));
	}

	private static Sex parseSex(String value) {
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "MALE", "M" -> Sex.MALE;
			case "FEMALE", "F" -> Sex.FEMALE;
			default -> null;
		};
	}

	private static MemberStatus parseStatus(String value) {
		String normalised = Normalizer.normalize(
				value.trim().toUpperCase(Locale.ROOT), Normalizer.Form.NFD);
		return switch (normalised) {
			case "ACTIVE", "ACTIF", "A" -> MemberStatus.ACTIVE;
			case "INACTIVE", "INACTIF" -> MemberStatus.INACTIVE;
			case "ARCHIVED", "ARCHIVE" -> MemberStatus.ARCHIVED;
			default -> null;
		};
	}

	private static LocalDate parseDate(String value) {
		for (DateTimeFormatter format : DATE_FORMATS) {
			try {
				return LocalDate.parse(value, format);
			} catch (DateTimeParseException exception) {
				continue;
			}
		}
		return null;
	}

	private static String normaliseHeader(String header) {
		String value = Normalizer.normalize(header.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "")
				.replaceAll("[^a-z0-9]+", "_");
		return value.replaceAll("^_+|_+$", "");
	}

	private static Map<String, String> headerAliases() {
		Map<String, String> aliases = new HashMap<>();
		alias(aliases, FIELD_LAST_NAME, "nom", "last_name", "lastname", "surname", "family_name", "nom_de_famille");
		alias(aliases, FIELD_FIRST_NAME, "prenom", "first_name", "firstname", "given_name", "forename");
		alias(aliases, FIELD_SEX, "sexe", "sex", "genre", "gender");
		alias(aliases, FIELD_BIRTH_DATE, "date_naissance", "birth_date", "birthdate", "date_de_naissance");
		alias(aliases, FIELD_PHONE, "telephone", "phone", "tel", "mobile", "numero_telephone");
		alias(aliases, FIELD_ADDRESS, "adresse", "address");
		alias(aliases, FIELD_STATUS, "statut", "status");
		alias(aliases, FIELD_JOINED_ON, "date_inscription", "joined_on", "date_adhesion", "date_debut");
		alias(aliases, FIELD_NOTES, "notes", "commentaire", "remarque", "observations");
		alias(aliases, FIELD_ENVELOPE_NUMBER, "numero_enveloppe", "envelope_number", "num_enveloppe", "enveloppe");
		return aliases;
	}

	private static void alias(Map<String, String> aliases, String canonical, String... names) {
		for (String name : names) {
			aliases.put(name, canonical);
		}
	}

	private static String truncate(String value) {
		if (value == null) {
			return null;
		}
		return value.length() <= 1000 ? value : value.substring(0, 1000);
	}

	private record CsvRecord(int lineNumber, String raw, List<String> cells) {
	}

	private record HeaderMapping(Map<String, Integer> columns, Set<String> ignoredFields) {
	}
}
