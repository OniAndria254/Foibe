package org.fam.dime.security;

import java.time.Instant;
import org.fam.dime.common.DimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminAccountInitializer.class);

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final DimeProperties dimeProperties;
	private final Environment environment;

	public AdminAccountInitializer(
			AppUserRepository appUserRepository,
			PasswordEncoder passwordEncoder,
			DimeProperties dimeProperties,
			Environment environment) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.dimeProperties = dimeProperties;
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (environment.acceptsProfiles(Profiles.of("test"))) {
			return;
		}
		if (appUserRepository.count() > 0) {
			return;
		}

		DimeProperties.Security security = dimeProperties.security();
		if (!security.hasInitialPassword()) {
			throw new IllegalStateException(
					"No user account exists and DIME_ADMIN_PASSWORD is not set. "
							+ "Add DIME_ADMIN_PASSWORD to .env (see .env.example), then restart.");
		}

		AppUser admin = new AppUser();
		admin.setUsername(security.initialAdminUsername());
		admin.setPasswordHash(passwordEncoder.encode(security.initialAdminPassword()));
		admin.setFullName("Administrateur");
		admin.setRole(UserRole.ADMIN);
		admin.setEnabled(true);
		Instant now = Instant.now();
		admin.setCreatedAt(now);
		admin.setUpdatedAt(now);
		appUserRepository.save(admin);

		LOGGER.info("Created initial ADMIN account '{}'", admin.getUsername());
	}
}
