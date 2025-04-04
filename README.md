## j4middle_intershop: Учебный проект магазина

### Требования:
- `JAVA 21`+

### Запуск локально:
- Обеспечить наличие развёрнутой БД `postgres` на `5432` порту, креды по-умолчанию `postgres:postgres`, задать другие значения можно в `application.properties`, блок `spring.datasource`
- MAC / LINUX:
    - Задать директорию для хранения изображений каталога в переменную окружения `INTERSHOP_ITEM_IMG_DIR`
    - Другие необходимые переменные:
      - `SPRING_DATASOURCE_URL` : r2dbc:postgresql://postgres:5432/postgres
      - `SPRING_DATASOURCE_USERNAME`: postgres
      - `SPRING_DATASOURCE_PASSWORD`: postgres
      - `SPRING_LIQUIBASE_URL`: jdbc:postgresql://postgres:5432/postgres
    - `./gradlew bootJar`
    - скопировать файл `intershop/build/libs/intershop-2.0.jar` в желаемую директорию (опционально)
    - `chmod +x intershop-2.0.jar`
    - `./intershop-2.0.jar`

-  WINDOWS
    - Задать директорию для хранения изображений каталога в переменную окружения `INTERSHOP_ITEM_IMG_DIR`
    - Другие необходимые переменные:
        - `SPRING_DATASOURCE_URL` : r2dbc:postgresql://postgres:5432/postgres
        - `SPRING_DATASOURCE_USERNAME`: postgres
        - `SPRING_DATASOURCE_PASSWORD`: postgres
        - `SPRING_LIQUIBASE_URL`: jdbc:postgresql://postgres:5432/postgres
    - скопировать файл `intershop\build\libs\intershop-2.0.jar` в желаемую директорию (опционально) 
    - `java -jar intershop-2.0.jar`

### Запуск в docker
- `docker build -t intershop:2.0 .`
- `docker-compose up --build`

### Детали использования: 
- Главная страница приложения - `http://localhost:8080/main/items`
- На главной странице есть кнопка ADMIN, ведущая к созданию товаров