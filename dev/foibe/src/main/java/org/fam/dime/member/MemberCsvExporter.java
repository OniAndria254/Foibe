package org.fam.dime.member;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.fam.dime.member.dto.MemberResponse;

public final class MemberCsvExporter {

	private static final String SEPARATOR = ";";

	private static final String BOM = "\uFEFF";

	private MemberCsvExporter() {
	}

	public static byte[] export(List<MemberResponse> members) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (Writer writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8)) {
			writer.write(BOM);
			writeRow(writer,
					"envelope_number", "last_name", "first_name", "sex", "birth_date",
					"phone", "address", "status", "joined_on", "notes");
			for (MemberResponse member : members) {
				writeRow(writer,
						String.valueOf(member.envelopeNumber()),
						member.lastName(),
						member.firstName(),
						member.sex() == null ? "" : member.sex().name(),
						member.birthDate() == null ? "" : member.birthDate().toString(),
						member.phone() == null ? "" : member.phone(),
						member.address() == null ? "" : member.address(),
						member.status().name(),
						member.joinedOn() == null ? "" : member.joinedOn().toString(),
						member.notes() == null ? "" : member.notes());
			}
			writer.flush();
		} catch (IOException exception) {
			throw new IllegalStateException("Cannot export members", exception);
		}
		return buffer.toByteArray();
	}

	private static void writeRow(Writer writer, String... cells) throws IOException {
		for (int index = 0; index < cells.length; index++) {
			if (index > 0) {
				writer.write(SEPARATOR);
			}
			writer.write(escape(cells[index]));
		}
		writer.write("\r\n");
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		boolean needsQuotes = value.contains(SEPARATOR)
				|| value.contains("\"")
				|| value.contains("\n")
				|| value.contains("\r");
		String escaped = value.replace("\"", "\"\"");
		return needsQuotes ? "\"" + escaped + "\"" : escaped;
	}
}
