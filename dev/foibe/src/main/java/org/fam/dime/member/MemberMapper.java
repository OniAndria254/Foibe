package org.fam.dime.member;

import org.fam.dime.member.dto.MemberResponse;

public final class MemberMapper {

	private MemberMapper() {
	}

	public static MemberResponse toResponse(Member member) {
		return new MemberResponse(
				member.getId(),
				member.getEnvelopeNumber(),
				member.getLastName(),
				member.getFirstName(),
				member.getSex(),
				member.getBirthDate(),
				member.getPhone(),
				member.getAddress(),
				member.getStatus(),
				member.getJoinedOn(),
				member.getNotes(),
				member.getCreatedAt());
	}
}
