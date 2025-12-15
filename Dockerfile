# Dockerfile optimisé pour Jenkins CI/CD avec tests
FROM maven:3.8.6-amazoncorretto-17 AS builder

WORKDIR /app

# 1. Copier le pom.xml d'abord (cache layer)
COPY pom.xml .

# 2. Télécharger toutes les dépendances (cache)
RUN mvn dependency:go-offline -B

# 3. Copier le code source
COPY src ./src

# 4. Exécuter les tests et générer le rapport JaCoCo
RUN mvn clean test jacoco:report

# 5. Build du package (sans tests, déjà faits)
RUN mvn clean package -DskipTests

# 6. Runtime image
FROM amazoncorretto:17-alpine

WORKDIR /app

# Copier uniquement le JAR
COPY --from=builder /app/target/*.jar app.jar

# Copier les rapports de test pour archivage (optionnel)
COPY --from=builder /app/target/surefire-reports /reports/surefire
COPY --from=builder /app/target/site/jacoco /reports/jacoco

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]