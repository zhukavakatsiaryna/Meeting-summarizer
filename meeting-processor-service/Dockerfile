FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY meeting-api-service/build.gradle ./meeting-api-service/
COPY meeting-processor-service/build.gradle ./meeting-processor-service/
RUN ./gradlew :meeting-processor-service:dependencies --no-daemon
COPY meeting-processor-service/src ./meeting-processor-service/src
RUN ./gradlew :meeting-processor-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/meeting-processor-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
