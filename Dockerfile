# ─────────────────────────────────────────────
# Stage 1 — build
# Scarica le dipendenze in uno strato separato:
# se pom.xml non cambia, Maven riusa la cache.
# ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline --no-transfer-progress

COPY src ./src
RUN mvn package -DskipTests --no-transfer-progress

# ─────────────────────────────────────────────
# Stage 2 — estrazione layer
# Spring Boot supporta il layered JAR:
# divide il contenuto in strati ordinati per
# frequenza di modifica (dipendenze → codice).
# Ogni strato cambia solo quando cambia il suo
# contenuto, massimizzando il cache hit in CI.
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS extractor

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ─────────────────────────────────────────────
# Stage 3 — runtime
# JRE-only alpine: immagine finale ~80MB
# invece dei ~350MB del JDK completo.
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# utente non-root: riduce la superficie d'attacco
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# copia i layer in ordine crescente di volatilità:
# le dipendenze cambiano raramente → strato basso e stabile
COPY --from=extractor /app/dependencies ./
COPY --from=extractor /app/spring-boot-loader ./
COPY --from=extractor /app/snapshot-dependencies ./
COPY --from=extractor /app/application ./

USER appuser

EXPOSE 8080

# profilo attivo sovrascrivibile a runtime:
# docker run -e SPRING_PROFILES_ACTIVE=prod ...
ENV SPRING_PROFILES_ACTIVE=dev

# UseContainerSupport: JVM rispetta i memory limits del container
# MaxRAMPercentage: usa al massimo il 75% della RAM assegnata
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
