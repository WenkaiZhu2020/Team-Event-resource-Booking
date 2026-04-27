FROM eclipse-temurin:21-jre
WORKDIR /app
COPY services/workflow-service/target/workflow-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
