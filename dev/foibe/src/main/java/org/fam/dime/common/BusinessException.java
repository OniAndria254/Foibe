package org.fam.dime.common;

import java.util.Arrays;
import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final Object[] args;

	protected BusinessException(String code, Object... args) {
		this(HttpStatus.BAD_REQUEST, code, args);
	}

	protected BusinessException(HttpStatus status, String code, Object... args) {
		super(args.length == 0 ? code : code + " " + Arrays.toString(args));
		this.status = status;
		this.code = code;
		this.args = args.clone();
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public Object[] getArgs() {
		return args.clone();
	}
}