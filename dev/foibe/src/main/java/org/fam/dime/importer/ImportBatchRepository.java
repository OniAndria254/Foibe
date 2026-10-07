package org.fam.dime.importer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

	Optional<ImportBatch> findByIdAndChurchId(Long id, Long churchId);
}
