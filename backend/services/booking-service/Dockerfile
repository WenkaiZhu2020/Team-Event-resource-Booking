FROM eclipse-temurin:21-jre

WORKDIR /app

COPY services/booking-service/target/booking-service-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
