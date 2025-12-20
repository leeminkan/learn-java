FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
# Note: For api-gateway Dockerfile, use EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]