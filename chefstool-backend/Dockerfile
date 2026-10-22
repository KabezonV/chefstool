# syntax=docker/dockerfile:1

# ---- Compilacion ----
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

# El wrapper fija la version de Maven del proyecto
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q package -DskipTests

# ---- Ejecucion ----
FROM eclipse-temurin:25-jre-noble

# Tesseract con diccionario espanol: necesario para leer albaranes en papel (fotos)
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-spa curl \
    && rm -rf /var/lib/apt/lists/* \
    && tesseract --list-langs

RUN useradd --system --uid 1001 --create-home chefstool \
    && mkdir -p /app/uploads \
    && chown -R chefstool:chefstool /app

WORKDIR /app
COPY --from=build --chown=chefstool:chefstool /app/target/*.jar app.jar

USER chefstool
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    CHEFSTOOL_UPLOADS=/app/uploads \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# La API responde 401 sin token: sirve como prueba de vida
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/yo | grep -q 401 || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
