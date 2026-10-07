package org.fam.dime.member;

import org.fam.dime.common.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateEnvelopeEntryException extends BusinessException {

	public DuplicateEnvelopeEntryException(int envelopeNumber) {
		super(HttpStatus.CONFLICT, "ENVELOPE_ALREADY_USED", envelopeNumber);
	}
}
