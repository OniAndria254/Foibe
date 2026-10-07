package org.fam.dime.security;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {

	private final CurrentUserService currentUserService;

	public CurrentUserAdvice(CurrentUserService currentUserService) {
		this.currentUserService = currentUserService;
	}

	@ModelAttribute("currentUser")
	public CurrentUser currentUser() {
		return currentUserService.currentOrAnonymous();
	}
}
