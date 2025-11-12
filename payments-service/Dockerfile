FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY payments-service/target/payments-service-*.jar app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
