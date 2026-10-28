# Builds SANA into one container: the React frontend is packaged inside the Spring Boot app,
# so the page and the API share one address (no CORS).

# 1) Build the React frontend
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# frontend/.env is not committed, so turn on API mode here
ENV VITE_SANA_MODE=api
RUN npm run build

# 2) Build the Spring Boot jar with the frontend files in static/
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -q -B package -Dmaven.test.skip=true

# 3) Run it with only a Java runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
