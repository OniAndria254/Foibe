package org.fam.dime.member;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

	Optional<Member> findByChurchIdAndEnvelopeNumber(Long churchId, Integer envelopeNumber);

	List<Member> findByChurchId(Long churchId);

	boolean existsByChurchIdAndEnvelopeNumber(Long churchId, Integer envelopeNumber);

	@Query("select max(m.envelopeNumber) from Member m where m.churchId = :churchId")
	Optional<Integer> findMaxEnvelopeNumberByChurchId(@Param("churchId") Long churchId);

	@Query("""
			select m from Member m
			 where m.churchId = :churchId
			   and lower(m.lastName) = lower(:lastName)
			   and lower(m.firstName) = lower(:firstName)
			""")
	List<Member> findByChurchIdAndNames(
			@Param("churchId") Long churchId,
			@Param("lastName") String lastName,
			@Param("firstName") String firstName);
}
