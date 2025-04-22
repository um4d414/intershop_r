package ru.umd.intershop.shop.data.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.NonNull;
import ru.umd.intershop.shop.common.constant.OrderStatusEnum;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class OrderEntity extends BaseEntity {
    @Id
    private Long id;

    /**
     * @see OrderStatusEnum
     */
    @Column
    @NonNull
    private String status;

    @Column
    @NonNull
    private BigDecimal totalPrice;

    public OrderEntity(@NonNull String status, @NonNull BigDecimal totalPrice) {
        this.status = status;
        this.totalPrice = totalPrice;
    }
}
