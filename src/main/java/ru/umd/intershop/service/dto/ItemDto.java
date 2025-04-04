package ru.umd.intershop.service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ItemDto {
    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private String imageFileName;

    public String getImagePath() {
        return "images/" + imageFileName;
    }
}
