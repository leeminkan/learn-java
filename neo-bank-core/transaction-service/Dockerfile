# CHANGE THIS LINE: Use JDK 23 (Alpine is lighter, but strict)
FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

# Ensure the wildcard matches your actual built jar name
COPY target/*-service-*.jar app.jar

EXPOSE 8081
# (Remember to use EXPOSE 8082 for the transaction-service file)

ENTRYPOINT ["java", "-jar", "app.jar"]