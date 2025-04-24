package ru.umd.intershop.payments.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.umd.intershop.payments.web.controller.exception.PaymentException;
import ru.umd.intershop.payments.web.model.*;

@RestController
@RequestMapping("/payments")
public class PaymentController implements PaymentsApi {
    private final double balance = 1000.0;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(new BalanceResponse(balance)));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> makePayment(
        Mono<PaymentRequest> paymentRequest,
        ServerWebExchange exchange
    ) {
        return paymentRequest
            .flatMap(request -> {
                double amount = request.getAmount();

                // Проверка на отрицательное значение
                if (amount < 0) {
                    return Mono.error(new PaymentException(
                        ErrorResponse.CodeEnum.INVALID_AMOUNT,
                        "Сумма платежа не может быть отрицательной"
                    ));
                }

                // Проверка на достаточность средств
                if (amount > balance) {
                    return Mono.error(new PaymentException(
                        ErrorResponse.CodeEnum.INSUFFICIENT_FUNDS,
                        "На счёте недостаточно средств"
                    ));
                }

                PaymentResponse response = new PaymentResponse();
                response.setSuccess(true);
                response.setRemainingBalance(balance - amount);

                return Mono.just(ResponseEntity.ok(response));
            });
    }
}