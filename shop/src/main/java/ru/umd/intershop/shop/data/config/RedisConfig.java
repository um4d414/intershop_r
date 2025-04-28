package ru.umd.intershop.shop.data.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;
import ru.umd.intershop.shop.data.cache.model.ItemCacheModel;
import ru.umd.intershop.shop.data.cache.model.ItemPageCacheModel;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class RedisConfig {
//    @Value("${spring.redis.host:localhost}")
//    private String redisHost;
//
//    @Value("${spring.redis.port:6379}")
//    private int redisPort;
//
//    @Bean
//    LettuceConnectionFactory redisConnectionFactory() {
//        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//
//        return new LettuceConnectionFactory(config);
//    }

    @Bean
    ReactiveRedisOperations<String, ItemCacheModel> itemRedisOperations(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ItemCacheModel> serializer = new Jackson2JsonRedisSerializer<>(ItemCacheModel.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, ItemCacheModel> builder =
            RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, ItemCacheModel> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    ReactiveRedisOperations<String, ItemPageCacheModel> itemPageRedisOperations(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ItemPageCacheModel> serializer = new Jackson2JsonRedisSerializer<>(
            ItemPageCacheModel.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, ItemPageCacheModel> builder =
            RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, ItemPageCacheModel> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

}
