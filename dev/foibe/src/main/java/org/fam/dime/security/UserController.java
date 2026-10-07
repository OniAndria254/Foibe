package org.fam.dime.security;

import java.time.Instant;
import java.util.List;
import org.fam.dime.audit.AuditAction;
import org.fam.dime.audit.AuditEntityType;
import org.fam.dime.audit.AuditLogService;
import org.fam.dime.security.dto.UserForm;
import org.fam.dime.security.dto.UserPasswordForm;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.fam.dime.common.BusinessException;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/users")
public class UserController {

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final CurrentUserService currentUserService;
	private final AuditLogService auditLogService;

	public UserController(
			AppUserRepository userRepository,
			PasswordEncoder passwordEncoder,
			CurrentUserService currentUserService,
			AuditLogService auditLogService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.currentUserService = currentUserService;
		this.auditLogService = auditLogService;
	}

	@ModelAttribute("roles")
	public UserRole[] roles() {
		return UserRole.values();
	}

	@GetMapping
	public String list(
			Model model,
			@PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
		model.addAttribute("users", userRepository.findAll(pageable));
		return "users/list";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		UserForm form = new UserForm();
		model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "userForm", new BeanPropertyBindingResult(form, "userForm"));
		model.addAttribute("userForm", form);
		return "users/form";
	}

	@PostMapping
	public String create(
			@Validated @ModelAttribute("userForm") UserForm form,
			BindingResult bindingResult,
			RedirectAttributes redirect) {
		if (bindingResult.hasErrors()) {
			return "users/form";
		}
		requireManagement(form.getRole());

		String username = form.getUsername();
		if (userRepository.existsByUsername(username)) {
			throw new UsernameAlreadyExistsException(username);
		}

		Long currentId = currentUserService.currentUserIdOrNull();
		Instant now = Instant.now();

		AppUser user = new AppUser();
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
		user.setFullName(form.getFullName());
		user.setRole(form.getRole());
		user.setEnabled(true);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		user.setCreatedBy(currentId);
		user.setUpdatedBy(currentId);
		AppUser saved = userRepository.save(user);
		auditLogService.recordChange(
				null, AuditEntityType.APP_USER, saved.getId(), AuditAction.CREATE, null, saved);
		redirect.addFlashAttribute("notify", "user.created");
		return "redirect:/users";
	}

	@PostMapping("/{id}/status")
	public String toggleStatus(@PathVariable Long id, RedirectAttributes redirect) {
		AppUser user = requireUser(id);
		Long currentId = currentUserService.currentUserIdOrNull();
		if (user.getId().equals(currentId)) {
			throw new AccessDeniedException("Cannot disable your own account");
		}
		requireManagement(user.getRole());
		user.setEnabled(!user.isEnabled());
		user.setUpdatedAt(Instant.now());
		user.setUpdatedBy(currentId);
		userRepository.save(user);
		auditLogService.recordChange(
				null, AuditEntityType.APP_USER, user.getId(), AuditAction.UPDATE, null, user);
		redirect.addFlashAttribute("notify", user.isEnabled() ? "user.enabled" : "user.disabled");
		return "redirect:/users";
	}

	@GetMapping("/{id}/password")
	public String passwordForm(@PathVariable Long id, Model model) {
		AppUser user = requireUser(id);
		requireManagement(user.getRole());
		model.addAttribute("target", user);
		UserPasswordForm form = new UserPasswordForm();
		model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "passwordForm", new BeanPropertyBindingResult(form, "passwordForm"));
		model.addAttribute("passwordForm", form);
		return "users/password";
	}

	@PostMapping("/{id}/password")
	public String resetPassword(
			@PathVariable Long id,
			@Validated @ModelAttribute("passwordForm") UserPasswordForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirect) {
		AppUser user = requireUser(id);
		requireManagement(user.getRole());
		if (bindingResult.hasErrors()) {
			model.addAttribute("target", user);
			return "users/password";
		}
		user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
		user.setUpdatedAt(Instant.now());
		user.setUpdatedBy(currentUserService.currentUserIdOrNull());
		userRepository.save(user);
		auditLogService.recordChange(
				null, AuditEntityType.APP_USER, user.getId(), AuditAction.UPDATE, null, user);
		redirect.addFlashAttribute("notify", "user.passwordReset");
		return "redirect:/users";
	}

	private AppUser requireUser(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new IllegalStateException("User " + id + " not found"));
	}

	private void requireManagement(UserRole target) {
		UserRole current = currentUserService.current().map(CurrentUser::role).orElse(null);
		if (target == UserRole.ADMIN && current != UserRole.ADMIN) {
			throw new AccessDeniedException("Only an admin can manage admin accounts");
		}
	}

	private static final class UsernameAlreadyExistsException extends BusinessException {

		private UsernameAlreadyExistsException(String username) {
			super(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", username);
		}
	}
}