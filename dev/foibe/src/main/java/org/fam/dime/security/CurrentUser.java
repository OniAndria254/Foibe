package org.fam.dime.security;

public record CurrentUser(String username, String fullName, UserRole role) {

	public static CurrentUser anonymous() {
		return new CurrentUser(null, null, null);
	}

	public boolean isAdmin() {
		return role == UserRole.ADMIN;
	}

	public boolean canManageMembers() {
		return role == UserRole.RESPONSIBLE || role == UserRole.ADMIN;
	}

	public boolean canAudit() {
		return canManageMembers();
	}
}
