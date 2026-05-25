FROM gradle:8.14.3-jdk21-alpine AS build

WORKDIR /workspace
COPY --chown=gradle:gradle . .
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
