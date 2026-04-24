FROM eclipse-temurin:21-jre
WORKDIR /app
COPY services/event-service/target/event-service-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
