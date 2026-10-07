package org.fam.dime.importer;

import org.fam.dime.common.BusinessException;
import org.springframework.http.HttpStatus;

public class ImportBatchNotPendingException extends BusinessException {

	public ImportBatchNotPendingException(Long id) {
		super(HttpStatus.CONFLICT, "IMPORT_BATCH_NOT_PENDING", id);
	}
}
