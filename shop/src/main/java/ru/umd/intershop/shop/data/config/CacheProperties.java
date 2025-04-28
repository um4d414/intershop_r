package ru.umd.intershop.shop.data.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Класс для хранения настроек кэша, значения загружаются из конфигурации приложения
 */
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    private final Ttl ttl = new Ttl();

    public Ttl getTtl() {
        return ttl;
    }

    public static class Ttl {
        /**
         * TTL для кэша страниц с товарами (в секундах)
         */
        private long itemsPage = 300; // По умолчанию 5 минут

        /**
         * TTL для кэша отдельных товаров (в секундах)
         */
        private long item = 600; // По умолчанию 10 минут

        public long getItemsPage() {
            return itemsPage;
        }

        public void setItemsPage(long itemsPage) {
            this.itemsPage = itemsPage;
        }

        public long getItem() {
            return item;
        }

        public void setItem(long item) {
            this.item = item;
        }
    }
}