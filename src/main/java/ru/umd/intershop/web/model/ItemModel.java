package ru.umd.intershop.web.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ItemModel {
    private Long id;

    private String imgPath;

    private String title;

    private BigDecimal price;

    private String description;

    private Integer count;
}
