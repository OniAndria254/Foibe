package org.fam.dime.member.dto;

import java.time.Instant;
import java.time.LocalDate;
import org.fam.dime.member.MemberStatus;
import org.fam.dime.member.Sex;

public record MemberResponse(
		Long id,
		Integer envelopeNumber,
		String lastName,
		String firstName,
		Sex sex,
		LocalDate birthDate,
		String phone,
		String address,
		MemberStatus status,
		LocalDate joinedOn,
		String notes,
		Instant createdAt) {

	public String fullName() {
		return lastName + " " + firstName;
	}
}
