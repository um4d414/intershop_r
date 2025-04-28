package ru.umd.intershop.payments.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.umd.intershop.payments.web.controller.exception.GlobalExceptionHandler;
import ru.umd.intershop.payments.web.model.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebFluxTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
public class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testGetBalance() {
        // Проверяем получение баланса
        webTestClient.get()
            .uri("/balance")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(BalanceResponse.class)
            .value(response -> {
                assertEquals(1000.0, response.getBalance(),
                             "Баланс должен быть равен 1000.0");
            });
    }

    @Test
    void testSuccessfulPayment() {
        // Создаем запрос на оплату с допустимой суммой
        PaymentRequest request = new PaymentRequest();
        request.setAmount(500.0);

        // Проверяем успешную оплату
        webTestClient.post()
            .uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(request), PaymentRequest.class)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(PaymentResponse.class)
            .value(response -> {
                assertTrue(response.getSuccess(),
                           "Платеж должен быть успешным");
                assertEquals(500.0, response.getRemainingBalance(),
                             "Оставшийся баланс должен быть 500.0");
            });
    }

    @Test
    void testPaymentWithMaximumAmount() {
        // Создаем запрос на оплату с максимально допустимой суммой
        PaymentRequest request = new PaymentRequest();
        request.setAmount(1000.0);

        // Проверяем оплату с максимальной суммой
        webTestClient.post()
            .uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(request), PaymentRequest.class)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(PaymentResponse.class)
            .value(response -> {
                assertTrue(response.getSuccess(),
                           "Платеж должен быть успешным");
                assertEquals(0.0, response.getRemainingBalance(),
                             "Оставшийся баланс должен быть 0.0");
            });
    }

    @Test
    void testPaymentWithInsufficientFunds() {
        // Создаем запрос на оплату с суммой, превышающей баланс
        PaymentRequest request = new PaymentRequest();
        request.setAmount(1500.0);

        // Проверяем отклонение платежа из-за недостатка средств
        webTestClient.post()
            .uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(request), PaymentRequest.class)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(response -> {
                assertEquals(ErrorResponse.CodeEnum.INSUFFICIENT_FUNDS, response.getCode(),
                             "Код ошибки должен быть INSUFFICIENT_FUNDS");
                assertEquals("На счёте недостаточно средств", response.getMessage(),
                             "Сообщение об ошибке должно содержать информацию о недостатке средств");
            });
    }

    @Test
    void testPaymentWithZeroAmount() {
        // Создаем запрос на оплату с нулевой суммой
        PaymentRequest request = new PaymentRequest();
        request.setAmount(0.0);

        // Проверяем платеж с нулевой суммой
        webTestClient.post()
            .uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(request), PaymentRequest.class)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(PaymentResponse.class)
            .value(response -> {
                assertTrue(response.getSuccess(),
                           "Платеж должен быть успешным");
                assertEquals(1000.0, response.getRemainingBalance(),
                             "Оставшийся баланс должен быть 1000.0");
            });
    }

    @Test
    void testPaymentWithNegativeAmount() {
        // Создаем запрос на оплату с отрицательной суммой
        PaymentRequest request = new PaymentRequest();
        request.setAmount(-100.0);

        // Проверяем отклонение платежа из-за отрицательной суммы
        webTestClient.post()
            .uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(request), PaymentRequest.class)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(response -> {
                assertEquals(ErrorResponse.CodeEnum.INVALID_AMOUNT, response.getCode(),
                             "Код ошибки должен быть INVALID_AMOUNT");
                assertEquals("Сумма платежа не может быть отрицательной", response.getMessage(),
                             "Сообщение об ошибке должно содержать информацию о некорректной сумме");
            });
    }

    @Test
    void testMalformedPaymentRequest() {
        // Проверяем обработку некорректного запроса (отсутствует поле amount)
        String malformedJson = "{}";

        webTestClient.post()
            .uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(malformedJson)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest();
    }
}