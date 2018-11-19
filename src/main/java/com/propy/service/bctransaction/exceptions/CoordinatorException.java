package com.propy.service.bctransaction.exceptions;

public class CoordinatorException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public CoordinatorException(String message, Throwable cause) {
		super(message, cause);
	}

	public CoordinatorException(Throwable cause) {
		super(cause);
	}
}
