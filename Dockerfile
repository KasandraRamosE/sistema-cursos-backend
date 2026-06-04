# ============================================================
# Dockerfile — Spring Boot Backend
# Ubicación: sistema-cursos-fhce/backend/sistema-cursos/Dockerfile
# ============================================================

# ------------------------------------
# ETAPA 1: BUILD
# Usa Maven + Java 21 para compilar el .jar
# ------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiamos pom.xml primero y descargamos dependencias
# Esto aprovecha el cache de Docker: si no cambia el pom.xml,
# no vuelve a descargar todo Maven en cada build
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------
# ETAPA 2: RUNTIME
# Imagen mucho más liviana — solo necesita Java para correr el .jar
# ------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos solo el .jar generado en la etapa anterior

COPY --from=build /app/target/sistema-cursos-0.0.1-SNAPSHOT.jar app.jar

# Creamos directorios para archivos generados por la app
# (certificados PDF, uploads, etc.)
RUN mkdir -p /app/certificados /app/uploads

# Puerto que expone Spring Boot
EXPOSE 8080

# Arranca con perfil "prod" — usa application-prod.yml
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]