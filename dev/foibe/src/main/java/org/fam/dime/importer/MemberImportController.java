package org.fam.dime.importer;

import java.io.IOException;
import org.fam.dime.importer.dto.ImportReportResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/members/import")
public class MemberImportController {

	private final MemberImportService importService;

	public MemberImportController(MemberImportService importService) {
		this.importService = importService;
	}

	@GetMapping
	public String uploadPage() {
		return "members/import/upload";
	}

	@PostMapping
	public String upload(
			@RequestParam("file") MultipartFile file,
			RedirectAttributes redirect) {
		long batchId = importService.analyse(sanitiseFilename(file.getOriginalFilename()), read(file));
		redirect.addFlashAttribute("importStarted", true);
		return "redirect:/members/import/report/" + batchId;
	}

	@GetMapping("/report/{batchId}")
	public String report(@PathVariable long batchId, Model model) {
		model.addAttribute("report", importService.report(batchId));
		return "members/import/report";
	}

	@PostMapping("/report/{batchId}/apply")
	public String apply(@PathVariable long batchId, RedirectAttributes redirect) {
		ImportReportResponse applied = importService.apply(batchId);
		redirect.addFlashAttribute("importApplied", true);
		redirect.addFlashAttribute("assignedFrom", applied.assignedFrom());
		redirect.addFlashAttribute("assignedTo", applied.assignedTo());
		return "redirect:/members/import/report/" + batchId;
	}

	@PostMapping("/report/{batchId}/cancel")
	public String cancel(@PathVariable long batchId, RedirectAttributes redirect) {
		importService.cancel(batchId);
		redirect.addFlashAttribute("importCancelled", true);
		return "redirect:/members/import/report/" + batchId;
	}

	private String sanitiseFilename(String original) {
		if (original == null || original.isBlank()) {
			return "import.csv";
		}
		int lastSlash = Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\'));
		String clean = lastSlash >= 0 ? original.substring(lastSlash + 1) : original;
		return clean.length() <= 255 ? clean : clean.substring(clean.length() - 255);
	}

	private byte[] read(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException exception) {
			throw new ImportFileException("IMPORT_FILE_READ_ERROR");
		}
	}
}