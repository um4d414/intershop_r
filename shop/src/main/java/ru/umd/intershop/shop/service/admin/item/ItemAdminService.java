package ru.umd.intershop.shop.service.admin.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.data.cache.model.ItemPageCacheModel;
import ru.umd.intershop.shop.data.entity.ItemEntity;
import ru.umd.intershop.shop.data.repository.ItemRepository;
import ru.umd.intershop.shop.service.admin.dto.ItemForm;

import java.io.File;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemAdminService {
    private final ItemRepository itemRepository;

    private final ReactiveRedisOperations<String, ItemPageCacheModel> itemPageRedisOperations;

    @Value("${app.image-file-base-path}")
    private String imageBasePath;

    public Mono<Void> createItem(ItemForm itemForm) {
        Mono<String> fileNameMono = processImageFile(itemForm.getImageFile());

        return fileNameMono.flatMap(fileName -> saveItemAndClearCache(itemForm, fileName));
    }

    private Mono<String> processImageFile(FilePart file) {
        if (file == null) {
            return Mono.just(null);
        }

        String originalFilename = file.filename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID() + extension;

        return Mono.fromCallable(() -> {
                File uploadDir = new File(imageBasePath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                return new File(uploadDir, newFileName).toPath();
            })
            .flatMap(path -> file.transferTo(path).thenReturn(newFileName))
            .onErrorResume(e -> {
                log.error("Ошибка при сохранении файла: {}", e.getMessage(), e);
                return Mono.error(new RuntimeException("Ошибка сохранения файла", e));
            });
    }

    private Mono<Void> saveItemAndClearCache(ItemForm itemForm, String imageFileName) {
        ItemEntity itemEntity = ItemEntity.builder()
            .name(itemForm.getName())
            .description(itemForm.getDescription())
            .price(itemForm.getPrice())
            .imageFileName(imageFileName)
            .isActive(itemForm.getIsActive() != null ? itemForm.getIsActive() : true)
            .build();

        return itemRepository.save(itemEntity)
            .flatMap(savedItem -> {
                log.info("Сохранен новый товар с ID: {}", savedItem.getId());
                return invalidateItemPageCache();
            })
            .onErrorResume(e -> {
                log.error("Ошибка при сохранении товара: {}", e.getMessage(), e);
                return Mono.error(new RuntimeException("Ошибка сохранения товара", e));
            });
    }

    private Mono<Void> invalidateItemPageCache() {
        log.info("Инвалидация кэша страниц товаров");

        return itemPageRedisOperations
            .scan()
            .filter(key -> key.toString().startsWith("items:page:"))
            .flatMap(key -> {
                log.debug("Удаление кэша для ключа: {}", key);
                return itemPageRedisOperations.opsForValue().delete(key);
            })
            .doOnComplete(() -> log.info("Кэш страниц товаров успешно инвалидирован"))
            .doOnError(e -> log.error("Ошибка при инвалидации кэша: {}", e.getMessage(), e))
            .onErrorResume(e -> {
                log.warn("Проблема при очистке кэша (игнорируется): {}", e.getMessage());
                return Mono.empty();
            })
            .then();
    }
}
