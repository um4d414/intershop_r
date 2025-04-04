package ru.umd.intershop.web.model;

import lombok.Builder;
import lombok.Setter;
import ru.umd.intershop.service.dto.ItemPageDto;

import java.util.Objects;

@Setter
@Builder
public class PagingModel {
    private Integer pageSize;

    private Integer pageNumber;

    private Boolean hasPrevious;

    private Boolean hasNext;

    public static PagingModel of(ItemPageDto items) {
        boolean hasPrevious = items.getPage() > 0;
        boolean hasNext = items.getPage() < items.getTotalPages() - 1;

        return PagingModel.builder()
            .pageSize(items.getPageSize())
            .pageNumber(items.getPage())
            .hasPrevious(hasPrevious)
            .hasNext(hasNext)
            .build();
    }
    public Integer pageSize() {
        return Objects.requireNonNullElse(this.pageSize, 0);
    }

    public Integer pageNumber() {
        return Objects.requireNonNullElse(this.pageNumber, 0);
    }

    public Boolean hasPrevious() {
        return Objects.requireNonNullElse(this.hasPrevious, false);
    }

    public Boolean hasNext() {
        return Objects.requireNonNullElse(this.hasNext, false);
    }
}
