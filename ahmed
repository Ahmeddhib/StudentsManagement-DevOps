# Dockerfile pour Spring Boot
# Étape 1: Builder l'application
FROM maven:3.8.6-openjdk-17-slim AS builder
WORKDIR /app

# Copier les fichiers de configuration
COPY pom.xml .
RUN mvn dependency:go-offline

# Copier le code source
COPY src ./src

# Builder l'application
RUN mvn clean package -DskipTests

# Étape 2: Créer l'image d'exécution
FROM openjdk:17-slim
WORKDIR /app

# Copier le JAR généré
COPY --from=builder /app/target/*.jar app.jar

# Exposition du port
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
