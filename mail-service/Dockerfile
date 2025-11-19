# Dockerfile for Mail Service
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/mail-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=k8s"]
