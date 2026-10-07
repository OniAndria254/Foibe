package org.fam.dime.member;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.fam.dime.audit.AuditAction;
import org.fam.dime.audit.AuditEntityType;
import org.fam.dime.audit.AuditLogService;
import org.fam.dime.church.ChurchService;
import org.fam.dime.common.DimeProperties;
import org.fam.dime.member.dto.CreateMemberRequest;
import org.fam.dime.member.dto.MemberResponse;
import org.fam.dime.member.dto.UpdateMemberRequest;
import org.fam.dime.security.CurrentUser;
import org.fam.dime.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

	private static final char LIKE_ESCAPE = '\\';

	private final MemberRepository memberRepository;
	private final ChurchService churchService;
	private final EnvelopeNumberService envelopeNumberService;
	private final AuditLogService auditLogService;
	private final CurrentUserService currentUserService;
	private final DimeProperties dimeProperties;

	public MemberService(
			MemberRepository memberRepository,
			ChurchService churchService,
			EnvelopeNumberService envelopeNumberService,
			AuditLogService auditLogService,
			CurrentUserService currentUserService,
			DimeProperties dimeProperties) {
		this.memberRepository = memberRepository;
		this.churchService = churchService;
		this.envelopeNumberService = envelopeNumberService;
		this.auditLogService = auditLogService;
		this.currentUserService = currentUserService;
		this.dimeProperties = dimeProperties;
	}

	@Transactional(readOnly = true)
	public Page<MemberResponse> search(MemberSearchCriteria criteria) {
		Long churchId = churchService.defaultChurchId();
		Sort sort = criteria.isSortByEnvelope()
				? Sort.by(Sort.Direction.ASC, "envelopeNumber")
				: Sort.by(Sort.Direction.ASC, "lastName", "firstName");
		Pageable pageable = PageRequest.of(criteria.pageOrDefault(), dimeProperties.pageSize(), sort);
		return memberRepository.findAll(buildSpecification(churchId, criteria), pageable)
				.map(MemberMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public MemberResponse get(Long id) {
		return MemberMapper.toResponse(require(id));
	}

	@Transactional(readOnly = true)
	public List<MemberResponse> findAllForExport() {
		return memberRepository
				.findAll(Sort.by(Sort.Direction.ASC, "envelopeNumber"))
				.stream()
				.map(MemberMapper::toResponse)
				.toList();
	}

	@Transactional
	public MemberResponse create(CreateMemberRequest request) {
		Long churchId = churchService.defaultChurchId();
		int envelopeNumber = envelopeNumberService.allocate(churchId);

		Member member = new Member();
		member.setChurchId(churchId);
		member.setEnvelopeNumber(envelopeNumber);
		applyIdentity(member, request);
		member.setStatus(request.getStatus() == null ? MemberStatus.ACTIVE : request.getStatus());
		member.setJoinedOn(request.getJoinedOn() == null ? LocalDate.now() : request.getJoinedOn());
		stampCreation(member);

		Member saved = memberRepository.save(member);
		auditLogService.recordChange(
				churchId, AuditEntityType.MEMBER, saved.getId(), AuditAction.CREATE,
				null, MemberMapper.toResponse(saved));
		return MemberMapper.toResponse(saved);
	}

	@Transactional
	public MemberResponse update(Long id, UpdateMemberRequest request) {
		Member member = require(id);

		if (request.getEnvelopeNumber() != null && !request.getEnvelopeNumber().equals(member.getEnvelopeNumber())) {
			changeEnvelopeNumber(member, request.getEnvelopeNumber());
		}

		MemberResponse before = MemberMapper.toResponse(member);
		applyIdentity(member, request);
		member.setStatus(request.getStatus());
		member.setJoinedOn(request.getJoinedOn() == null ? member.getJoinedOn() : request.getJoinedOn());
		stampUpdate(member);

		Member saved = memberRepository.save(member);
		auditLogService.recordChange(
				saved.getChurchId(), AuditEntityType.MEMBER, saved.getId(), AuditAction.UPDATE,
				before, MemberMapper.toResponse(saved));
		return MemberMapper.toResponse(saved);
	}

	@Transactional
	public MemberResponse changeStatus(Long id, MemberStatus status) {
		Member member = require(id);
		MemberStatus before = member.getStatus();
		if (before == status) {
			return MemberMapper.toResponse(member);
		}

		member.setStatus(status);
		stampUpdate(member);
		Member saved = memberRepository.save(member);

		auditLogService.recordChange(
				saved.getChurchId(), AuditEntityType.MEMBER, saved.getId(), AuditAction.STATUS_CHANGE,
				before.name(), status.name());
		return MemberMapper.toResponse(saved);
	}

	private void changeEnvelopeNumber(Member member, int requestedNumber) {
		if (!isAdmin()) {
			throw new AccessDeniedException("Only an admin can change an envelope number");
		}
		envelopeNumberService.registerExplicit(member.getChurchId(), requestedNumber);
		member.setEnvelopeNumber(requestedNumber);
	}

	private Member require(Long id) {
		return memberRepository.findById(id)
				.orElseThrow(() -> new MemberNotFoundException(id));
	}

	private boolean isAdmin() {
		return currentUserService.current().map(CurrentUser::isAdmin).orElse(false);
	}

	private void applyIdentity(Member member, CreateMemberRequest request) {
		applyIdentity(
				member,
				request.getLastName(),
				request.getFirstName(),
				request.getSex(),
				request.getBirthDate(),
				request.getPhone(),
				request.getAddress(),
				request.getJoinedOn(),
				request.getNotes());
	}

	private void applyIdentity(Member member, UpdateMemberRequest request) {
		applyIdentity(
				member,
				request.getLastName(),
				request.getFirstName(),
				request.getSex(),
				request.getBirthDate(),
				request.getPhone(),
				request.getAddress(),
				request.getJoinedOn(),
				request.getNotes());
	}

	private void applyIdentity(
			Member member,
			String lastName,
			String firstName,
			Sex sex,
			LocalDate birthDate,
			String phone,
			String address,
			LocalDate joinedOn,
			String notes) {
		member.setLastName(lastName == null ? null : lastName.trim());
		member.setFirstName(firstName == null ? null : firstName.trim());
		member.setSex(sex);
		member.setBirthDate(birthDate);
		member.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
		member.setAddress(address == null || address.isBlank() ? null : address.trim());
		member.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
		if (joinedOn != null) {
			member.setJoinedOn(joinedOn);
		}
	}

	private void stampCreation(Member member) {
		Instant now = Instant.now();
		member.setCreatedAt(now);
		member.setUpdatedAt(now);
		member.setCreatedBy(currentUserService.currentUserIdOrNull());
		member.setUpdatedBy(member.getCreatedBy());
	}

	private void stampUpdate(Member member) {
		member.setUpdatedAt(Instant.now());
		member.setUpdatedBy(currentUserService.currentUserIdOrNull());
	}

	private Specification<Member> buildSpecification(Long churchId, MemberSearchCriteria criteria) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("churchId"), churchId));

			criteria.statusOrDefault()
					.ifPresent(status -> predicates.add(cb.equal(root.get("status"), status)));

			String text = normalize(criteria.query());
			if (!text.isEmpty()) {
				predicates.add(buildTextPredicate(root.get("lastName"), root.get("firstName"),
						root.get("envelopeNumber"), text, cb));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private Predicate buildTextPredicate(
			Expression<String> lastName,
			Expression<String> firstName,
			Expression<Integer> envelopeNumber,
			String text,
			CriteriaBuilder cb) {

		String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
		Predicate nameMatch = cb.or(
				cb.like(cb.lower(lastName), pattern, LIKE_ESCAPE),
				cb.like(cb.lower(firstName), pattern, LIKE_ESCAPE));

		if (!text.chars().allMatch(Character::isDigit)) {
			return nameMatch;
		}
		try {
			return cb.or(nameMatch, cb.equal(envelopeNumber, Integer.valueOf(text)));
		} catch (NumberFormatException exception) {
			return nameMatch;
		}
	}

	private String normalize(String text) {
		return text == null ? "" : text.trim();
	}

	private String escapeLike(String text) {
		return text
				.replace(String.valueOf(LIKE_ESCAPE), String.valueOf(LIKE_ESCAPE).repeat(2))
				.replace("%", LIKE_ESCAPE + "%")
				.replace("_", LIKE_ESCAPE + "_");
	}
}
