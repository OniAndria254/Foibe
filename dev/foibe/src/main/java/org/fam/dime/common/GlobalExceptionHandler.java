package org.fam.dime.common;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final MessageSource messageSource;

	public GlobalExceptionHandler(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@ExceptionHandler(BusinessException.class)
	public ModelAndView business(BusinessException exception, HttpServletResponse response, Locale locale) {
		response.setStatus(exception.getStatus().value());
		return errorView(
				exception.getStatus(),
				code("error.business.title", locale),
				message(exception.getCode(), exception.getArgs(), locale),
				locale);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ModelAndView denied(AccessDeniedException exception, HttpServletResponse response, Locale locale) {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		return errorView(
				HttpStatus.FORBIDDEN,
				code("error.403.title", locale),
				code("error.403.message", locale),
				locale);
	}

	@ExceptionHandler(BindException.class)
	public ModelAndView binding(BindException exception, HttpServletResponse response, Locale locale) {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		String detail = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> message(error.getDefaultMessage(), new Object[0], locale))
				.findFirst()
				.orElseGet(() -> code("error.business.title", locale));
		return errorView(HttpStatus.BAD_REQUEST, code("error.business.title", locale), detail, locale);
	}

	@ExceptionHandler(Exception.class)
	public ModelAndView unexpected(Exception exception, HttpServletResponse response, Locale locale) {
		log.error("Unexpected error", exception);
		response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		return errorView(
				HttpStatus.INTERNAL_SERVER_ERROR,
				code("error.500.title", locale),
				code("error.500.message", locale),
				locale);
	}

	private ModelAndView errorView(HttpStatus status, String title, String message, Locale locale) {
		ModelAndView view = new ModelAndView("error/error");
		view.addObject("status", status.value());
		view.addObject("title", title);
		view.addObject("message", message);
		view.addObject("backUrl", code("error.backUrl", locale));
		return view;
	}

	private String message(String code, Object[] args, Locale locale) {
		return messageSource.getMessage(code, args, code, locale);
	}

	private String code(String code, Locale locale) {
		return messageSource.getMessage(code, null, code, locale);
	}
}