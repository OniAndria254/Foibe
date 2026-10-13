package org.fam.dime.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserPasswordForm {

	@NotBlank(message = "user.password.required")
	@Size(min = 5, max = 72, message = "user.password.invalid")
	private String password;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}