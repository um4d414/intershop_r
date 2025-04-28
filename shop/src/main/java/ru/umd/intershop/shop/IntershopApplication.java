package ru.umd.intershop.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableCaching
public class IntershopApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntershopApplication.class, args);
    }

}
