package org.fam.dime.member;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MemberExportFileNamesTest {

	@Test
	void buildsCsvNameWithIsoDate() {
		assertThat(MemberExportFileNames.memberExport("csv", LocalDate.of(2026, 10, 13)))
				.isEqualTo("Membres_Foibe_2026-10-13.csv");
	}

	@Test
	void buildsXlsxNameWithIsoDate() {
		assertThat(MemberExportFileNames.memberExport("xlsx", LocalDate.of(2026, 10, 13)))
				.isEqualTo("Membres_Foibe_2026-10-13.xlsx");
	}
}