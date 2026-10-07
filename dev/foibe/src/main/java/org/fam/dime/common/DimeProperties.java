package org.fam.dime.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dime")
public record DimeProperties(
		int pageSize,
		int importMaxRows,
		int inactivityAlertMonths,
		Security security) {

	public record Security(String initialAdminUsername, String initialAdminPassword) {

		public boolean hasInitialPassword() {
			return initialAdminPassword != null && !initialAdminPassword.isBlank();
		}
	}
}
