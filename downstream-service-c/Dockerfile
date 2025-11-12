FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml ./common-lib/
COPY common-lib/src ./common-lib/src
COPY downstream-service-c/pom.xml ./downstream-service-c/
COPY downstream-service-c/src ./downstream-service-c/src
# Copy POM files for other modules to satisfy parent POM (no source needed)
COPY sts-service/pom.xml ./sts-service/
COPY gateway-service/pom.xml ./gateway-service/
COPY payments-service/pom.xml ./payments-service/
COPY payments-worker-service/pom.xml ./payments-worker-service/
RUN mvn clean package -DskipTests -pl downstream-service-c,common-lib -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/downstream-service-c/target/downstream-service-c-*.jar app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
