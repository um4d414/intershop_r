package ru.umd.intershop.payments.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.umd.intershop.payments.web.model.*;

@Controller
public class PaymentController implements PaymentsApi {

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
        return null;
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> makePayment(
        Mono<PaymentRequest> paymentRequest,
        ServerWebExchange exchange
    ) {
        return null;
    }
}