FROM gradle:8.6-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre AS shop
WORKDIR /app
COPY --from=builder /app/shop/build/libs/shop.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre AS payments
WORKDIR /app
COPY --from=builder /app/payments/build/libs/payments.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]