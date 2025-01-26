# syntax=docker/dockerfile:1
# Сборка проекта осуществляется изолированно внутри контейнера, для проверки, что не зависим от локальных зависимостей
# Используем контейнер с jdk21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
# Создаём папку для исходников
RUN mkdir -p /src
# Копируем только конфигурационные файлы (pom.xml и settings.xml)
ADD ./*.xml /src/
# Скачиваем зависимости указанные в pom.xml из репозиториев указанных в settings.xml (например корпоративных)
RUN ["/usr/local/bin/mvn-entrypoint.sh", "mvn", "--s", "/src/settings.xml", "--f", "/src/pom.xml", "dependency:go-offline", "--fail-never", "-Dmaven.artifact.threads=32", "install", "clean", "-T", "32", "-B"]
# Копируем исходники приложения
COPY ./ /src/
# Запускаем сборку в оффлайн моде
RUN ["mvn", "package", "--f", "/src/pom.xml", "--s", "/src/settings.xml"]

# Используем контейнер с jre21 как более легковестный
FROM eclipse-temurin:21-jre-alpine AS runner
# Создаем папку для приложения
WORKDIR /app
# Копируем результаты сборки в папку с приложением
COPY --from=builder ./src/target/*.jar ./app.jar
# Документируем порт на котором будет работать приложение
EXPOSE 8002
# Запускаем приложение
ENTRYPOINT ["java","-Dspring.profiles.active=prod","-jar","./app.jar"]
