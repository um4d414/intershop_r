package ru.umd.intershop.web.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderModel {
    private Long id;

    private List<ItemModel> items;

    private String total;
}
