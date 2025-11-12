FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml ./common-lib/
COPY common-lib/src ./common-lib/src
COPY payments-worker-service/pom.xml ./payments-worker-service/
COPY payments-worker-service/src ./payments-worker-service/src
# Copy POM files for other modules to satisfy parent POM (no source needed)
COPY sts-service/pom.xml ./sts-service/
COPY gateway-service/pom.xml ./gateway-service/
COPY payments-service/pom.xml ./payments-service/
COPY downstream-service-c/pom.xml ./downstream-service-c/
RUN mvn clean package -DskipTests -pl payments-worker-service,common-lib -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/payments-worker-service/target/payments-worker-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
