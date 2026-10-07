package org.fam.dime.importer;

import org.fam.dime.common.BusinessException;

public class ImportFileException extends BusinessException {

	public ImportFileException(String code, Object... args) {
		super(code, args);
	}
}
