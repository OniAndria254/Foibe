package org.fam.dime.member;

import org.fam.dime.common.BusinessException;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends BusinessException {

	public MemberNotFoundException(Long id) {
		super(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", id);
	}
}
