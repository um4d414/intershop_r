FROM openjdk:21-jdk-slim AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean bootJar

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/intershop-2.0.jar app.jar
EXPOSE 8080
RUN chmod +x app.jar
ENTRYPOINT ["./app.jar"]