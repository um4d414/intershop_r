package ru.umd.intershop.payments.web.controller.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;
import ru.umd.intershop.payments.web.model.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePaymentException(PaymentException ex) {
        return Mono.just(ResponseEntity.badRequest().body(ex.getErrorResponse()));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse(
            ErrorResponse.CodeEnum.INVALID_AMOUNT,
            "Произошла ошибка при обработке запроса: " + ex.getMessage()
        );
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }
}