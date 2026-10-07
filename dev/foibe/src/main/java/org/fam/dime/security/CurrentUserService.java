package org.fam.dime.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

	private final AppUserRepository appUserRepository;

	public CurrentUserService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	public Optional<AppUser> currentAppUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}
		if (authentication.getPrincipal() instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		return appUserRepository.findByUsername(authentication.getName());
	}

	public Optional<CurrentUser> current() {
		return currentAppUser()
				.map(user -> new CurrentUser(user.getUsername(), user.getFullName(), user.getRole()));
	}

	public CurrentUser currentOrAnonymous() {
		return current().orElseGet(CurrentUser::anonymous);
	}

	public Long currentUserIdOrNull() {
		return currentAppUser().map(AppUser::getId).orElse(null);
	}

	public Optional<Long> findUserIdByUsername(String username) {
		return appUserRepository.findByUsername(username).map(AppUser::getId);
	}
}
