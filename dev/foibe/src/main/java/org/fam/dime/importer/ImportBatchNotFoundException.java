package org.fam.dime.importer;

import org.fam.dime.common.BusinessException;
import org.springframework.http.HttpStatus;

public class ImportBatchNotFoundException extends BusinessException {

	public ImportBatchNotFoundException(Long id) {
		super(HttpStatus.NOT_FOUND, "IMPORT_BATCH_NOT_FOUND", id);
	}
}
