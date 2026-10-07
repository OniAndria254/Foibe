package org.fam.dime.importer;

import java.time.LocalDate;
import java.util.Locale;
import org.fam.dime.member.MemberStatus;
import org.fam.dime.member.Sex;

public record ParsedMemberRow(
		int lineNumber,
		String rawLine,
		String lastName,
		String firstName,
		Sex sex,
		LocalDate birthDate,
		String phone,
		String address,
		MemberStatus status,
		LocalDate joinedOn,
		String notes) {

	public String normalisedNameKey() {
		return (lastName + '|' + firstName).toLowerCase(Locale.ROOT);
	}
}
