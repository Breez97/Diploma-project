# --- Packaging ---
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

COPY pom.xml .

COPY api-gateway ./api-gateway/
COPY service-search ./service-search/
COPY common-library ./common-library/
# COPY service-authentication ./service-authentication/
# COPY service-vaadin ./service-vaadin/

RUN mvn package -pl api-gateway -am -DskipTests -Dmaven.test.skip=true --batch-mode

# --- Execution ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/api-gateway/target/api-gateway.jar /app/api-gateway.jar
ENTRYPOINT ["java", "-jar", "/app/api-gateway.jar"]