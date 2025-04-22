package ru.umd.intershop.shop.service.admin.item;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import ru.umd.intershop.shop.data.entity.ItemEntity;
import ru.umd.intershop.shop.data.repository.ItemRepository;
import ru.umd.intershop.shop.service.admin.dto.ItemForm;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemAdminService {
    private final ItemRepository itemRepository;

    @Value("${app.image-file-base-path}")
    private String imageBasePath;

    public void createItem(ItemForm itemForm) {
        FilePart file = itemForm.getImageFile();
        String newFileName = null;

        if (file != null) {
            // Используем file.filename() вместо getOriginalFilename()
            String originalFilename = file.filename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            newFileName = UUID.randomUUID() + extension;

            try {
                File uploadDir = new File(imageBasePath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                File dest = new File(uploadDir, newFileName);
                // FilePart.transferTo требует Path, поэтому преобразуем dest
                Path destination = dest.toPath();
                // transferTo возвращает Mono<Void>; подписываемся, чтобы выполнить операцию
                file.transferTo(destination).subscribe();
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сохранения файла", e);
            }
        }

        ItemEntity itemEntity = ItemEntity.builder()
            .name(itemForm.getName())
            .description(itemForm.getDescription())
            .price(itemForm.getPrice())
            .imageFileName(newFileName)
            .isActive(itemForm.getIsActive() != null ? itemForm.getIsActive() : true)
            .build();

        itemRepository.save(itemEntity).subscribe();
    }
}
