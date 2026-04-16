# Stage 1: Build the JAR file using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline --no-transfer-progress
COPY src ./src
RUN mvn clean package -DskipTests --no-transfer-progress

# Stage 2: Run the JAR in a lightweight container
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/Web-Based-Financial-management-App-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]