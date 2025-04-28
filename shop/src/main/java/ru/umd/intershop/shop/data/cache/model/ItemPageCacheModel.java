package ru.umd.intershop.shop.data.cache.model;

import java.util.List;

public record ItemPageCacheModel(
    List<ItemCacheModel> items,
    int totalItems,
    int totalPages,
    int page,
    int pageSize,
    String sortField,
    String searchQuery,
    long createdAt
) {
    public ItemPageCacheModel(
        List<ItemCacheModel> items,
        int totalItems,
        int totalPages,
        int page,
        int pageSize
    ) {
        this(items, totalItems, totalPages, page, pageSize, "NO", null, System.currentTimeMillis());
    }

    public static ItemPageCacheModel create(
        List<ItemCacheModel> items,
        int totalItems,
        int totalPages,
        int page,
        int pageSize,
        String sortField,
        String searchQuery
    ) {
        return new ItemPageCacheModel(
            items,
            totalItems,
            totalPages,
            page,
            pageSize,
            sortField,
            searchQuery,
            System.currentTimeMillis()
        );
    }
}
