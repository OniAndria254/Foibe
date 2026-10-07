package org.fam.dime.importer;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.text.Collator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.fam.dime.audit.AuditAction;
import org.fam.dime.audit.AuditEntityType;
import org.fam.dime.audit.AuditLogService;
import org.fam.dime.church.ChurchService;
import org.fam.dime.common.DimeProperties;
import org.fam.dime.importer.dto.ImportPreviewRow;
import org.fam.dime.importer.dto.ImportReportResponse;
import org.fam.dime.importer.dto.ImportRowErrorResponse;
import org.fam.dime.member.EnvelopeNumberService;
import org.fam.dime.member.Member;
import org.fam.dime.member.MemberRepository;
import org.fam.dime.security.AppUser;
import org.fam.dime.security.AppUserRepository;
import org.fam.dime.security.CurrentUserService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberImportService {

	private static final int PREVIEW_LIMIT = 20;
	private static final Collator NAME_COLLATOR = Collator.getInstance(Locale.FRANCE);

	private final ImportBatchRepository importBatchRepository;
	private final ImportRowErrorRepository importRowErrorRepository;
	private final MemberRepository memberRepository;
	private final AppUserRepository appUserRepository;
	private final ChurchService churchService;
	private final EnvelopeNumberService envelopeNumberService;
	private final AuditLogService auditLogService;
	private final CurrentUserService currentUserService;
	private final MessageSource messageSource;
	private final DimeProperties dimeProperties;
	private final ObjectMapper objectMapper;

	public MemberImportService(
			ImportBatchRepository importBatchRepository,
			ImportRowErrorRepository importRowErrorRepository,
			MemberRepository memberRepository,
			AppUserRepository appUserRepository,
			ChurchService churchService,
			EnvelopeNumberService envelopeNumberService,
			AuditLogService auditLogService,
			CurrentUserService currentUserService,
			MessageSource messageSource,
			DimeProperties dimeProperties,
			ObjectMapper objectMapper) {
		this.importBatchRepository = importBatchRepository;
		this.importRowErrorRepository = importRowErrorRepository;
		this.memberRepository = memberRepository;
		this.appUserRepository = appUserRepository;
		this.churchService = churchService;
		this.envelopeNumberService = envelopeNumberService;
		this.auditLogService = auditLogService;
		this.currentUserService = currentUserService;
		this.messageSource = messageSource;
		this.dimeProperties = dimeProperties;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public Long analyse(String filename, byte[] content) {
		Long churchId = churchService.defaultChurchId();
		String sourceText = MemberCsvParser.decodeUtf8(content);
		CsvParseResult parsed = MemberCsvParser.parse(sourceText);
		if (parsed.totalRows() > dimeProperties.importMaxRows()) {
			throw new ImportFileException("IMPORT_TOO_MANY_ROWS", dimeProperties.importMaxRows());
		}
		Analysis analysis = analyseForChurch(churchId, parsed);

		Long userId = currentUserService.currentUserIdOrNull();
		Instant now = Instant.now();

		ImportBatch batch = new ImportBatch();
		batch.setChurchId(churchId);
		batch.setFilename(filename);
		batch.setSourceText(sourceText);
		batch.setStatus(ImportBatchStatus.PENDING);
		batch.setTotalRows(parsed.totalRows());
		batch.setValidRows(analysis.acceptedRows().size());
		batch.setErrorRows(analysis.rowErrors().size());
		batch.setIgnoredColumns(String.join(", ", analysis.ignoredColumns()));
		batch.setPreviewMembers(previewJson(analysis.acceptedRows()));
		batch.setStartedBy(userId);
		batch.setStartedAt(now);
		batch.setCreatedAt(now);
		batch.setUpdatedAt(now);
		batch.setCreatedBy(userId);
		batch.setUpdatedBy(userId);
		ImportBatch saved = importBatchRepository.save(batch);

		for (CsvRowError error : analysis.rowErrors()) {
			ImportRowError rowError = new ImportRowError();
			rowError.setBatchId(saved.getId());
			rowError.setLineNumber(error.lineNumber());
			rowError.setRawLine(error.rawLine());
			rowError.setErrorMessage(message(error.code(), error.args()));
			rowError.setCreatedAt(now);
			importRowErrorRepository.save(rowError);
		}
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public ImportReportResponse report(Long batchId) {
		Long churchId = churchService.defaultChurchId();
		ImportBatch batch = require(batchId, churchId);
		return toReport(batch);
	}

	@Transactional
	public ImportReportResponse apply(Long batchId) {
		Long churchId = churchService.defaultChurchId();
		ImportBatch batch = require(batchId, churchId);
		if (batch.getStatus() != ImportBatchStatus.PENDING) {
			throw new ImportBatchNotPendingException(batchId);
		}

		Analysis analysis = analyseForChurch(churchId, MemberCsvParser.parse(batch.getSourceText()));
		if (analysis.acceptedRows().isEmpty()) {
			throw new ImportFileException("IMPORT_NO_VALID_ROWS");
		}

		EnvelopeNumberService.EnvelopeBlock block =
				envelopeNumberService.reserveBlock(churchId, analysis.acceptedRows().size());
		int number = block.from();
		Long userId = currentUserService.currentUserIdOrNull();
		Instant now = Instant.now();

		for (ParsedMemberRow row : analysis.acceptedRows()) {
			Member member = new Member();
			member.setChurchId(churchId);
			member.setEnvelopeNumber(number++);
			member.setLastName(row.lastName());
			member.setFirstName(row.firstName());
			member.setSex(row.sex());
			member.setBirthDate(row.birthDate());
			member.setPhone(row.phone());
			member.setAddress(row.address());
			member.setStatus(row.status());
			member.setJoinedOn(row.joinedOn() != null ? row.joinedOn() : LocalDate.now());
			member.setNotes(row.notes());
			member.setCreatedAt(now);
			member.setUpdatedAt(now);
			member.setCreatedBy(userId);
			member.setUpdatedBy(userId);
			memberRepository.save(member);
		}

		batch.setStatus(ImportBatchStatus.APPLIED);
		batch.setValidRows(analysis.acceptedRows().size());
		batch.setErrorRows(analysis.rowErrors().size());
		batch.setAssignedFrom(block.from());
		batch.setAssignedTo(block.to());
		batch.setAppliedAt(now);
		batch.setUpdatedAt(now);
		batch.setUpdatedBy(userId);
		importBatchRepository.save(batch);

		ImportReportResponse report = toReport(batch);
		auditLogService.recordChange(
				churchId, AuditEntityType.IMPORT_BATCH, batch.getId(),
				AuditAction.IMPORT, null, report);
		return report;
	}

	@Transactional
	public ImportReportResponse cancel(Long batchId) {
		Long churchId = churchService.defaultChurchId();
		ImportBatch batch = require(batchId, churchId);
		if (batch.getStatus() != ImportBatchStatus.PENDING) {
			throw new ImportBatchNotPendingException(batchId);
		}
		batch.setStatus(ImportBatchStatus.CANCELLED);
		batch.setUpdatedAt(Instant.now());
		batch.setUpdatedBy(currentUserService.currentUserIdOrNull());
		importBatchRepository.save(batch);
		return toReport(batch);
	}

	private Analysis analyseForChurch(Long churchId, CsvParseResult parsed) {
		List<CsvRowError> errors = new ArrayList<>(parsed.rowErrors());
		Map<String, Integer> knownEnvelopes = new HashMap<>();
		memberRepository.findByChurchId(churchId).forEach(member -> knownEnvelopes.put(
				(member.getLastName() + '|' + member.getFirstName()).toLowerCase(Locale.ROOT),
				member.getEnvelopeNumber()));

		Set<String> seenInFile = new HashSet<>();
		List<ParsedMemberRow> accepted = new ArrayList<>();

		for (ParsedMemberRow row : parsed.validRows()) {
			String key = row.normalisedNameKey();
			if (!seenInFile.add(key)) {
				errors.add(new CsvRowError(
						row.lineNumber(), row.rawLine(), "IMPORT_ROW_DUPLICATE_IN_FILE", List.of()));
				continue;
			}
			Integer existing = knownEnvelopes.get(key);
			if (existing != null) {
				errors.add(new CsvRowError(
						row.lineNumber(), row.rawLine(), "IMPORT_ROW_ALREADY_REGISTERED",
						List.of(existing)));
				continue;
			}
			accepted.add(row);
		}

		accepted.sort(Comparator.comparing(ParsedMemberRow::lastName, NAME_COLLATOR)
				.thenComparing(ParsedMemberRow::firstName, NAME_COLLATOR)
				.thenComparingInt(ParsedMemberRow::lineNumber));
		errors.sort(Comparator.comparingInt(CsvRowError::lineNumber));
		return new Analysis(accepted, errors, parsed.ignoredColumns(), parsed.totalRows());
	}

	private ImportReportResponse toReport(ImportBatch batch) {
		List<ImportRowErrorResponse> errors = importRowErrorRepository
				.findByBatchIdOrderByLineNumber(batch.getId())
				.stream()
				.map(error -> new ImportRowErrorResponse(
						error.getLineNumber(), error.getRawLine(), error.getErrorMessage()))
				.toList();
		List<ImportPreviewRow> preview = previewRows(batch);

		return new ImportReportResponse(
				batch.getId(),
				batch.getFilename(),
				batch.getStatus(),
				batch.getTotalRows(),
				batch.getValidRows(),
				batch.getErrorRows(),
				batch.getAssignedFrom(),
				batch.getAssignedTo(),
				splitIgnored(batch.getIgnoredColumns()),
				username(batch.getStartedBy()),
				batch.getStartedAt(),
				batch.getAppliedAt(),
				errors,
				preview);
	}

	private List<ImportPreviewRow> previewRows(ImportBatch batch) {
		List<ImportedName> names = previewNames(batch.getPreviewMembers());
		if (batch.getStatus() == ImportBatchStatus.CANCELLED || names.isEmpty()) {
			return List.of();
		}
		int start = batch.getAssignedFrom() != null
				? batch.getAssignedFrom()
				: envelopeNumberService.previewNextNumber(batch.getChurchId());
		List<ImportPreviewRow> preview = new ArrayList<>();
		for (int index = 0; index < Math.min(names.size(), PREVIEW_LIMIT); index++) {
			ImportedName name = names.get(index);
			preview.add(new ImportPreviewRow(
					start + index, name.lastName(), name.firstName(), name.lineNumber()));
		}
		return preview;
	}

	private String previewJson(List<ParsedMemberRow> rows) {
		List<ImportedName> names = rows.stream()
				.map(row -> new ImportedName(
						row.lineNumber(), row.lastName(), row.firstName()))
				.toList();
		try {
			return objectMapper.writeValueAsString(names);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Cannot serialise import preview", exception);
		}
	}

	private List<ImportedName> previewNames(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(
					json,
					objectMapper.getTypeFactory()
							.constructCollectionType(List.class, ImportedName.class));
		} catch (JacksonException exception) {
			return List.of();
		}
	}

	private String username(Long userId) {
		if (userId == null) {
			return null;
		}
		return appUserRepository.findById(userId).map(AppUser::getUsername).orElse(null);
	}

	private List<String> splitIgnored(String ignoredColumns) {
		if (ignoredColumns == null || ignoredColumns.isBlank()) {
			return List.of();
		}
		return List.of(ignoredColumns.split("\\s*,\\s*"));
	}

	private String message(String code, List<Object> args) {
		return messageSource.getMessage(code, args.toArray(), LocaleContextHolder.getLocale());
	}

	private ImportBatch require(Long batchId, Long churchId) {
		return importBatchRepository.findByIdAndChurchId(batchId, churchId)
				.orElseThrow(() -> new ImportBatchNotFoundException(batchId));
	}

	private record Analysis(
			List<ParsedMemberRow> acceptedRows,
			List<CsvRowError> rowErrors,
			List<String> ignoredColumns,
			int totalRows) {
	}

	private record ImportedName(int lineNumber, String lastName, String firstName) {
	}
}