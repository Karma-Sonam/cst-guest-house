# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
COPY docker docker
# CI runs unit and PostgreSQL integration tests before this image build.
RUN ./mvnw -B -ntp -DskipTests package
RUN javac -d /healthcheck docker/HealthCheck.java

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd --system guesthouse && useradd --system --gid guesthouse --home-dir /app guesthouse
COPY --from=build --chown=guesthouse:guesthouse /workspace/target/guesthouse-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --from=build --chown=guesthouse:guesthouse /healthcheck/HealthCheck.class /app/HealthCheck.class
USER guesthouse
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=6 CMD ["java","-cp","/app","HealthCheck"]
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
