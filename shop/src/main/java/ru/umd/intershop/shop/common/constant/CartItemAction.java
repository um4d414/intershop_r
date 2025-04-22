package ru.umd.intershop.shop.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CartItemAction {
    PLUS,
    MINUS,
    DELETE
}
