## j4middle_intershop: Учебный проект магазина

### Требования:
- `JAVA 21`+
- `docker`

### Запуск Fat-JAR:
- Обеспечить наличие развёрнутой БД `postgres` на `5432` порту, креды по-умолчанию `postgres:postgres`, задать другие значения можно в `shop/resources/application.yml`
- Обеспечить наличие развёрнутого хранилища `redis` на `6379` порту, задать другие значения можно в `shop/resources/application.yml`
- MAC / LINUX:
    - Задать директорию для хранения изображений каталога в переменную окружения `INTERSHOP_ITEM_IMG_DIR`
    - Другие необходимые переменные:
      - `SPRING_DATASOURCE_URL` : r2dbc:postgresql://postgres:5432/postgres
      - `SPRING_DATASOURCE_USERNAME`: postgres
      - `SPRING_DATASOURCE_PASSWORD`: postgres
      - `SPRING_LIQUIBASE_URL`: jdbc:postgresql://postgres:5432/postgres
      - `APP_PAYMENTS_SERVICE_URL`: адрес хоста приложения оплаты `payments`, если оно разворачивается на той же машине, адрес по умолчанию `localhost`
    - `./gradlew bootJar`
    - `chmod +x ./shop/build/libs/shop.jar`
    - `chmod +x ./payments/build/libs/payments.jar`
    - `./shop/build/libs/shop.jar`
    - `./payments/build/libs/payments.jar`

### Запуск в docker
- `docker-compose up --build`

### Детали использования: 
- Главная страница приложения - `http://localhost:8080/main/items`
- На главной странице есть кнопка ADMIN, ведущая к созданию товаров