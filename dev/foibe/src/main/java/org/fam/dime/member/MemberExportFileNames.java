package org.fam.dime.member;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class MemberExportFileNames {

	private static final String PREFIX = "Membres_Foibe_";

	private MemberExportFileNames() {
	}

	public static String memberExport(String extension) {
		return memberExport(extension, LocalDate.now());
	}

	static String memberExport(String extension, LocalDate date) {
		return PREFIX + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + "." + extension;
	}
}