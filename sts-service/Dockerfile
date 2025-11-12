FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml ./common-lib/
COPY common-lib/src ./common-lib/src
COPY sts-service/pom.xml ./sts-service/
COPY sts-service/src ./sts-service/src
# Copy POM files for other modules to satisfy parent POM (no source needed)
COPY gateway-service/pom.xml ./gateway-service/
COPY payments-service/pom.xml ./payments-service/
COPY payments-worker-service/pom.xml ./payments-worker-service/
COPY downstream-service-c/pom.xml ./downstream-service-c/
RUN mvn clean package -DskipTests -pl sts-service,common-lib -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/sts-service/target/sts-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
