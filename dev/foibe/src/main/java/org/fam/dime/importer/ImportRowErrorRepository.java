package org.fam.dime.importer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportRowErrorRepository extends JpaRepository<ImportRowError, Long> {

	List<ImportRowError> findByBatchIdOrderByLineNumber(Long batchId);
}
