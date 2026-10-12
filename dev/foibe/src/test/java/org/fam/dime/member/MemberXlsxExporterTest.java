package org.fam.dime.member;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fam.dime.member.dto.MemberResponse;
import org.junit.jupiter.api.Test;

class MemberXlsxExporterTest {

	@Test
	void exportWritesHeaderAndMembersInGivenOrder() throws Exception {
		MemberResponse first = member(1L, 3, "RAKOTO", "Jean");
		MemberResponse second = member(2L, 1, "RAZAFY", "Marie");

		byte[] bytes = MemberXlsxExporter.export(
				List.of(first, second), "N° d'enveloppe", "Nom", "Prénom");

		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
			Sheet sheet = workbook.getSheet("Membres");
			assertThat(sheet).isNotNull();
			assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("N° d'enveloppe");
			assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Nom");
			assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Prénom");
			assertThat(sheet.getLastRowNum()).isEqualTo(2);

			assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(3.0);
			assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("RAKOTO");
			assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Jean");
			assertThat(sheet.getRow(2).getCell(0).getNumericCellValue()).isEqualTo(1.0);
			assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("RAZAFY");
			assertThat(sheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("Marie");
		}
	}

	private MemberResponse member(Long id, int envelopeNumber, String lastName, String firstName) {
		return new MemberResponse(
				id, envelopeNumber, lastName, firstName,
				null, null, null, null, MemberStatus.ACTIVE, null, null, null);
	}
}