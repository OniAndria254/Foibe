package org.fam.dime.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.fam.dime.security.UserRole;

public class UserForm {

	@NotBlank(message = "user.username.required")
	@Pattern(regexp = "[a-zA-Z0-9._]{3,50}", message = "user.username.invalid")
	private String username;

	@NotBlank(message = "user.password.required")
	@Size(min = 8, max = 72, message = "user.password.invalid")
	private String password;

	@NotBlank(message = "user.fullName.required")
	@Size(max = 120, message = "user.fullName.invalid")
	private String fullName;

	@NotNull(message = "user.role.required")
	private UserRole role;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public UserRole getRole() {
		return role;
	}

	public void setRole(UserRole role) {
		this.role = role;
	}
}