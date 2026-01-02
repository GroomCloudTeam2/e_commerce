package com.groom.e_commerce.payment.presentation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class TossApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String tossErrorCode;

	public TossApiException(HttpStatusCode statusCode, String code, String message, String tossErrorCode) {
		super(message);
		this.status = HttpStatus.valueOf(statusCode.value());
		this.code = code;
		this.tossErrorCode = tossErrorCode;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getTossErrorCode() {
		return tossErrorCode;
	}
}
