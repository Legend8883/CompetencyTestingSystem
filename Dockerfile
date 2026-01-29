# Dockerfile с использованием Gradle
FROM gradle:8.6.0-jdk21-alpine AS builder

WORKDIR /app

# Копируем Gradle файлы для кэширования
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
# Убрал gradle.properties или добавьте условие

# Даем права на выполнение gradlew
RUN chmod +x gradlew

# Копируем исходники для кэширования зависимостей
COPY src ./src

# Загружаем зависимости (кешируется отдельно от сборки)
RUN ./gradlew dependencies --no-daemon

# Собираем JAR
RUN ./gradlew build --no-daemon -x test

# Финальный образ
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

# Копируем собранный JAR из стадии builder
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# Оптимизированный запуск для контейнеров
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]