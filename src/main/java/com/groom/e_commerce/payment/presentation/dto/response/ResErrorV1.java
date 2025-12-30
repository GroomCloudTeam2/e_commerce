package com.groom.e_commerce.payment.presentation.dto.response;

public record ResErrorV1(
	String code,
	String message,
	String tossErrorCode
) {
	public ResErrorV1(String code, String message) {
		this(code, message, null);
	}

	public ResErrorV1(String code, String message, String tossErrorCode) {
		this.code = code;
		this.message = message;
		this.tossErrorCode = tossErrorCode;
	}
}
