package org.fam.dime.receipt;

import java.time.LocalDate;
import org.fam.dime.church.ChurchService;
import org.fam.dime.member.MemberService;
import org.fam.dime.member.dto.MemberResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/receipts")
public class ReceiptController {

	private final MemberService memberService;
	private final ChurchService churchService;

	public ReceiptController(MemberService memberService, ChurchService churchService) {
		this.memberService = memberService;
		this.churchService = churchService;
	}

	@GetMapping("/preview/{memberId}")
	public String preview(@PathVariable Long memberId, Model model) {
		MemberResponse member = memberService.get(memberId);
		model.addAttribute("receipt", new ReceiptView(
				churchService.defaultChurch().getName(),
				member,
				LocalDate.now()));
		return "receipts/receipt";
	}
}