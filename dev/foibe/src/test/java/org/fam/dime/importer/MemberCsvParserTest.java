package org.fam.dime.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.fam.dime.member.MemberStatus;
import org.fam.dime.member.Sex;
import org.junit.jupiter.api.Test;

class MemberCsvParserTest {

	@Test
	void parsesSemicolonSeparatedFrenchHeadersWithAccents() {
		String csv = """
				NOM;PRÉNOM;SEXE;DATE DE NAISSANCE;TÉLÉPHONE;ADRESSE
				Rakoto;Nirina;M;15/03/1988;0321234567;Antananarivo
				Andria;Feno;F;22/11/1995;;Itaosy
				""";

		CsvParseResult result = MemberCsvParser.parse(csv);

		assertThat(result.totalRows()).isEqualTo(2);
		assertThat(result.rowErrors()).isEmpty();
		assertThat(result.validRows()).hasSize(2);

		ParsedMemberRow first = result.validRows().get(0);
		assertThat(first.lastName()).isEqualTo("Rakoto");
		assertThat(first.firstName()).isEqualTo("Nirina");
		assertThat(first.sex()).isEqualTo(Sex.MALE);
		assertThat(first.birthDate()).isEqualTo(LocalDate.of(1988, 3, 15));
		assertThat(first.phone()).isEqualTo("0321234567");
		assertThat(first.address()).isEqualTo("Antananarivo");
	}

	@Test
	void detectsCommaSeparatorAndKeepsQuotedFieldsIntact() {
		String csv = "last_name,first_name,envelope_number\n\"Randria, Marie\",Haja,5\n";

		CsvParseResult result = MemberCsvParser.parse(csv);

		assertThat(result.ignoredColumns()).contains("envelope_number");
		ParsedMemberRow row = result.validRows().get(0);
		assertThat(row.lastName()).isEqualTo("Randria, Marie");
		assertThat(row.firstName()).isEqualTo("Haja");
	}

	@Test
	void detectsTabSeparator() {
		String csv = "nom\tprenom\tstatut\nRasoa\tMiora\tactif\n";

		CsvParseResult result = MemberCsvParser.parse(csv);

		assertThat(result.validRows()).hasSize(1);
		assertThat(result.validRows().get(0).status()).isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	void handlesCrlfAndDefaultValues() {
		String csv = "nom;prenom;sexe\r\nRakotomanga;Hery;M\r\n";

		CsvParseResult result = MemberCsvParser.parse(csv);

		ParsedMemberRow row = result.validRows().get(0);
		assertThat(row.status()).isEqualTo(MemberStatus.ACTIVE);
		assertThat(row.birthDate()).isNull();
	}

	@Test
	void rejectsInvalidSexDateAndMissingNameWithRowErrors() {
		String csv = """
				nom;prenom;sexe;date_naissance
				Rakoto;Valid;M;15/03/1988
				Zanaka;BadSex;X;01/01/2000
				Papa;BadDate;F;not-a-date
				;;;;15/03/1988
				""";

		CsvParseResult result = MemberCsvParser.parse(csv);

		assertThat(result.totalRows()).isEqualTo(4);
		assertThat(result.validRows()).hasSize(1);
		assertThat(result.rowErrors())
				.extracting(CsvRowError::code)
				.containsExactlyInAnyOrder(
						"IMPORT_ROW_INVALID_SEX",
						"IMPORT_ROW_INVALID_BIRTH_DATE",
						"IMPORT_ROW_MISSING_LAST_NAME");
		assertThat(result.rowErrors()).extracting(CsvRowError::lineNumber).hasSize(3);
	}

	@Test
	void rejectsNonUtf8Bytes() {
		byte[] latin1 = "nom;prenom\nRakoto;é".getBytes(StandardCharsets.ISO_8859_1);

		assertThatThrownBy(() -> MemberCsvParser.decodeUtf8(latin1))
				.isInstanceOf(ImportFileException.class)
				.hasFieldOrPropertyWithValue("code", "IMPORT_FILE_NOT_UTF8");
	}

	@Test
	void rejectsEmptyAndHeaderOnlyFiles() {
		assertThatThrownBy(() -> MemberCsvParser.parse(""))
				.isInstanceOf(ImportFileException.class);
		assertThatThrownBy(() -> MemberCsvParser.parse("nom;prenom"))
				.isInstanceOf(ImportFileException.class);
	}
}