package org.fam.dime.member;

import org.fam.dime.church.Church;
import org.fam.dime.church.ChurchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvelopeNumberService {

	private final ChurchRepository churchRepository;
	private final MemberRepository memberRepository;

	public EnvelopeNumberService(ChurchRepository churchRepository, MemberRepository memberRepository) {
		this.churchRepository = churchRepository;
		this.memberRepository = memberRepository;
	}

	@Transactional
	public int allocate(Long churchId) {
		return reserveBlock(churchId, 1).from();
	}

	@Transactional
	public EnvelopeBlock reserveBlock(Long churchId, int count) {
		if (count < 1) {
			throw new IllegalArgumentException("count must be positive");
		}
		Church church = lock(churchId);
		int from = nextAvailableNumber(church, churchId);
		church.setEnvelopeNextNumber(from + count);
		churchRepository.save(church);
		return new EnvelopeBlock(from, from + count - 1);
	}

	@Transactional
	public void registerExplicit(Long churchId, int envelopeNumber) {
		Church church = lock(churchId);
		if (memberRepository.existsByChurchIdAndEnvelopeNumber(churchId, envelopeNumber)) {
			throw new DuplicateEnvelopeEntryException(envelopeNumber);
		}
		if (envelopeNumber >= church.getEnvelopeNextNumber()) {
			church.setEnvelopeNextNumber(envelopeNumber + 1);
			churchRepository.save(church);
		}
	}

	@Transactional(readOnly = true)
	public int previewNextNumber(Long churchId) {
		Church church = churchRepository.findById(churchId)
				.orElseThrow(() -> new IllegalStateException("Church " + churchId + " not found"));
		return nextAvailableNumber(church, churchId);
	}

	private Church lock(Long churchId) {
		return churchRepository.findByIdForUpdate(churchId)
				.orElseThrow(() -> new IllegalStateException("Church " + churchId + " not found"));
	}

	private int nextAvailableNumber(Church church, Long churchId) {
		int afterExistingMax = memberRepository.findMaxEnvelopeNumberByChurchId(churchId).orElse(0) + 1;
		return Math.max(church.getEnvelopeNextNumber(), afterExistingMax);
	}

	public record EnvelopeBlock(int from, int to) {

		public int size() {
			return to - from + 1;
		}
	}
}
