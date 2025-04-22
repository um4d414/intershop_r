package ru.umd.intershop.shop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.umd.intershop.shop.config.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IntershopApplicationTests {
    @Test
    void contextLoads() {
    }
}
