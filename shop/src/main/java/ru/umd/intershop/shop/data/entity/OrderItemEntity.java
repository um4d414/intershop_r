package ru.umd.intershop.shop.data.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items")
public class OrderItemEntity extends BaseEntity {
    @Id
    private Long id;

    @Column
    private Long orderId;

    @Column
    private Long itemId;

    @Column
    private int count;
}