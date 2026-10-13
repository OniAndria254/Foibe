package org.fam.dime.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/login",
								"/error",
								"/css/**",
								"/js/**",
								"/images/**",
								"/webjars/**")
						.permitAll()
						.requestMatchers("/users/**", "/audit/**")
						.hasAnyRole(UserRole.RESPONSIBLE.name(), UserRole.ADMIN.name())
						.requestMatchers(
								"/members/new",
								"/members/*/edit",
								"/members/*/status",
								"/members/import",
								"/members/import/*/apply",
								"/members/import/*/cancel",
								"/members/export",
								"/members/export/xlsx")
						.hasAnyRole(UserRole.RESPONSIBLE.name(), UserRole.ADMIN.name())
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/members", true)
						.permitAll())
				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
