package org.fam.dime.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.fam.dime.church.ChurchService;
import org.fam.dime.importer.dto.ImportReportResponse;
import org.fam.dime.importer.dto.ImportRowErrorResponse;
import org.fam.dime.member.Member;
import org.fam.dime.member.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class MemberImportFlowTest {

	@Autowired
	private MemberImportService importService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ChurchService churchService;

	@Autowired
	private ImportBatchRepository batchRepository;

	@Test
	void analyseThenApplyInsertsMembersAndAssignsEnvelopes() {
		String csv = """
				nom;prenom;sexe;date_naissance;telephone;adresse;statut
				Rakotomanga;Hery;M;15/03/1988;0324587965;Ambatomaro;actif
				Andria;Feno;F;22/11/1995;;Itaosy;
				Rasolofoniaina;Vony;F;30/01/2001;0331122334;Ampasamadinika;actif
				""";

		Long batchId = importService.analyse("membres.csv", csv.getBytes(StandardCharsets.UTF_8));

		ImportReportResponse pending = importService.report(batchId);
		assertThat(pending.status()).isEqualTo(ImportBatchStatus.PENDING);
		assertThat(pending.validRows()).isEqualTo(3);
		assertThat(pending.errorRows()).isZero();
		assertThat(pending.previewRows()).hasSize(3);

		ImportReportResponse applied = importService.apply(batchId);

		assertThat(applied.status()).isEqualTo(ImportBatchStatus.APPLIED);
		assertThat(applied.assignedFrom()).isEqualTo(1);
		assertThat(applied.assignedTo()).isEqualTo(3);
		assertThat(batchRepository.findById(batchId).orElseThrow().getAssignedFrom()).isEqualTo(1);
		assertThat(batchRepository.findById(batchId).orElseThrow().getAssignedTo()).isEqualTo(3);

		Long churchId = churchService.defaultChurchId();
		List<Member> members = memberRepository.findByChurchId(churchId);
		assertThat(members)
				.extracting(Member::getLastName)
				.containsExactlyInAnyOrder("Rakotomanga", "Andria", "Rasolofoniaina");
		assertThat(members)
				.extracting(Member::getEnvelopeNumber)
				.containsExactlyInAnyOrder(1, 2, 3);
	}

	@Test
	void duplicateRowsInFileAreRejectedDuringAnalysis() {
		String csv = """
				nom;prenom
				Rakoto;Nirina
				Rakoto;Nirina
				""";

		Long batchId = importService.analyse("doublons.csv", csv.getBytes(StandardCharsets.UTF_8));
		ImportReportResponse report = importService.report(batchId);

		assertThat(report.validRows()).isEqualTo(1);
		assertThat(report.errorRows()).isEqualTo(1);
		assertThat(report.rowErrors())
				.extracting(ImportRowErrorResponse::message)
				.hasSize(1)
				.allMatch(message -> !message.isBlank());
	}

	@Test
	void cancelLeavesBatchCancelledAndInsertsNoMembers() {
		String csv = """
				nom;prenom
				Razafy;Liva
				""";

		Long batchId = importService.analyse("annule.csv", csv.getBytes(StandardCharsets.UTF_8));
		ImportReportResponse cancelled = importService.cancel(batchId);

		assertThat(cancelled.status()).isEqualTo(ImportBatchStatus.CANCELLED);
		Long churchId = churchService.defaultChurchId();
		assertThat(memberRepository.findByChurchId(churchId))
				.extracting(Member::getLastName)
				.doesNotContain("Razafy");
	}
}