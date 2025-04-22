package ru.umd.intershop.shop.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemSortingEnum {
    NO("id"),
    ALPHA("name"),
    PRICE("price");

    private final String entityField;
}
