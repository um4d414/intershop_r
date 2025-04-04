package ru.umd.intershop.service.admin.dto;

import lombok.Data;
import org.springframework.http.codec.multipart.FilePart;

import java.math.BigDecimal;

@Data
public class ItemForm {
    private String name;

    private String description;

    private BigDecimal price;

    private FilePart imageFile;

    private Boolean isActive = true;
}
