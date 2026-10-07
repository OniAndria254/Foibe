package org.fam.dime.member;

import java.util.Optional;

public record MemberSearchCriteria(String query, String status, String sort, Integer page) {

	public static final String SORT_ENVELOPE = "envelope";

	private static final String SORT_NAME = "name";

	public String sortOrDefault() {
		return SORT_ENVELOPE.equals(sort) ? SORT_ENVELOPE : SORT_NAME;
	}

	public int pageOrDefault() {
		return page == null || page < 0 ? 0 : page;
	}

	public Optional<MemberStatus> statusOrDefault() {
		if (status == null || status.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(MemberStatus.valueOf(status));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	public boolean isSortByEnvelope() {
		return SORT_ENVELOPE.equals(sortOrDefault());
	}
}
