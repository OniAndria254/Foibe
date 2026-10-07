package org.fam.dime.church;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChurchRepository extends JpaRepository<Church, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Church c where c.id = :id")
	Optional<Church> findByIdForUpdate(@Param("id") Long id);

	Optional<Church> findFirstByOrderByIdAsc();
}
