FROM maven:3.9.9-eclipse-temurin-17 AS construccion
WORKDIR /compilacion
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ENV TZ=America/Guatemala
WORKDIR /app
COPY --from=construccion /compilacion/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
