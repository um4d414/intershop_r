package ru.umd.intershop.shop.data.cache.model;

import org.springframework.data.redis.core.RedisHash;

import java.math.BigDecimal;

@RedisHash(value = "item", timeToLive = 60)
public record ItemCacheModel(
    String id,
    String name,
    BigDecimal price,
    String description,
    String imageFileName
) {
}
