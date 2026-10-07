package org.fam.dime.church;

import org.springframework.stereotype.Service;

@Service
public class ChurchService {

	private final ChurchRepository churchRepository;

	public ChurchService(ChurchRepository churchRepository) {
		this.churchRepository = churchRepository;
	}

	public Church defaultChurch() {
		return churchRepository.findFirstByOrderByIdAsc()
				.orElseThrow(() -> new IllegalStateException("No church is configured"));
	}

	public Long defaultChurchId() {
		return defaultChurch().getId();
	}

	public Church requireById(Long id) {
		return churchRepository.findById(id)
				.orElseThrow(() -> new IllegalStateException("Church " + id + " not found"));
	}

	public Church lockForEnvelopeAllocation(Long id) {
		return churchRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new IllegalStateException("Church " + id + " not found"));
	}
}
