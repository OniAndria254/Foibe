package org.fam.dime.member;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.fam.dime.member.dto.CreateMemberRequest;
import org.fam.dime.member.dto.MemberResponse;
import org.fam.dime.member.dto.UpdateMemberRequest;
import org.fam.dime.security.CurrentUser;
import org.fam.dime.security.CurrentUserService;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/members")
public class MemberController {

	private final MemberService memberService;
	private final MessageSource messageSource;
	private final CurrentUserService currentUserService;

	public MemberController(
			MemberService memberService,
			MessageSource messageSource,
			CurrentUserService currentUserService) {
		this.memberService = memberService;
		this.messageSource = messageSource;
		this.currentUserService = currentUserService;
	}

	@ModelAttribute("statuses")
	public MemberStatus[] statuses() {
		return MemberStatus.values();
	}

	@ModelAttribute("sexes")
	public Sex[] sexes() {
		return Sex.values();
	}

	@GetMapping
	public String list(@ModelAttribute("criteria") MemberSearchCriteria criteria, Model model) {
		Page<MemberResponse> members = memberService.search(criteria);
		model.addAttribute("members", members);
		model.addAttribute("canManage", canManage());
		return "members/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		CreateMemberRequest form = new CreateMemberRequest();
		form.setStatus(MemberStatus.ACTIVE);
		form.setJoinedOn(LocalDate.now());
		model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "form", new BeanPropertyBindingResult(form, "form"));
		model.addAttribute("form", form);
		model.addAttribute("action", "/members/new");
		model.addAttribute("editing", false);
		return "members/form";
	}

	@PostMapping("/new")
	public String create(
			@Valid @ModelAttribute("form") CreateMemberRequest form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		model.addAttribute("action", "/members/new");
		model.addAttribute("editing", false);
		if (bindingResult.hasErrors()) {
			return "members/form";
		}
		try {
			MemberResponse created = memberService.create(form);
			redirectAttributes.addFlashAttribute("successMessage",
					message("member.flash.created", created.envelopeNumber()));
			return "redirect:/members/" + created.id();
		} catch (DuplicateEnvelopeEntryException exception) {
			bindingResult.reject(exception.getCode(), exception.getMessage());
			return "members/form";
		}
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("member", memberService.get(id));
		model.addAttribute("canManage", canManage());
		return "members/detail";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		MemberResponse member = memberService.get(id);
		UpdateMemberRequest form = toUpdateRequest(member);
		model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "form", new BeanPropertyBindingResult(form, "form"));
		model.addAttribute("form", form);
		model.addAttribute("action", "/members/" + id + "/edit");
		model.addAttribute("editing", true);
		return "members/form";
	}

	@PostMapping("/{id}/edit")
	public String update(
			@PathVariable Long id,
			@Valid @ModelAttribute("form") UpdateMemberRequest form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		model.addAttribute("action", "/members/" + id + "/edit");
		model.addAttribute("editing", true);
		if (bindingResult.hasErrors()) {
			return "members/form";
		}
		try {
			MemberResponse updated = memberService.update(id, form);
			redirectAttributes.addFlashAttribute("successMessage", message("member.flash.updated"));
			return "redirect:/members/" + updated.id();
		} catch (DuplicateEnvelopeEntryException exception) {
			bindingResult.reject(exception.getCode(), exception.getMessage());
			return "members/form";
		}
	}

	@PostMapping("/{id}/status")
	public String changeStatus(
			@PathVariable Long id,
			@RequestParam MemberStatus status,
			RedirectAttributes redirectAttributes) {
		MemberResponse updated = memberService.changeStatus(id, status);
		redirectAttributes.addFlashAttribute("successMessage", message("member.flash.statusChanged"));
		return "redirect:/members/" + updated.id();
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> export() {
		byte[] csv = MemberCsvExporter.export(memberService.findAllForExport());
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"membres.csv\"")
				.contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
				.body(csv);
	}

	private UpdateMemberRequest toUpdateRequest(MemberResponse member) {
		UpdateMemberRequest form = new UpdateMemberRequest();
		form.setEnvelopeNumber(member.envelopeNumber());
		form.setLastName(member.lastName());
		form.setFirstName(member.firstName());
		form.setSex(member.sex());
		form.setBirthDate(member.birthDate());
		form.setPhone(member.phone());
		form.setAddress(member.address());
		form.setStatus(member.status());
		form.setJoinedOn(member.joinedOn());
		form.setNotes(member.notes());
		return form;
	}

	private boolean canManage() {
		return currentUserService.current().map(CurrentUser::canManageMembers).orElse(false);
	}

	private String message(String key, Object... arguments) {
		return messageSource.getMessage(key, arguments, org.springframework.context.i18n.LocaleContextHolder.getLocale());
	}
}
