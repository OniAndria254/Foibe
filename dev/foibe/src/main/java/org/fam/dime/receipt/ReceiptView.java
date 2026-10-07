package org.fam.dime.receipt;

import java.time.LocalDate;
import org.fam.dime.member.dto.MemberResponse;

public record ReceiptView(
		String churchName,
		MemberResponse member,
		LocalDate issueDate) {
}