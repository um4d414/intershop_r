package ru.umd.intershop.shop.data.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "items")
public class ItemEntity extends BaseEntity {
    @Id
    private Long id;

    @NonNull
    private String name;

    private String description;

    private BigDecimal price;

    private String imageFileName;

    @NonNull
    private Boolean isActive;
}
