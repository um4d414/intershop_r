package ru.umd.intershop.payments.web.controller.exception;

import ru.umd.intershop.payments.web.model.ErrorResponse;

public class PaymentException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public PaymentException(ErrorResponse.CodeEnum code, String message) {
        super(message);
        this.errorResponse = new ErrorResponse(code, message);
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }
}
