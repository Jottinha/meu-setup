# ── Stage 1: Compila o binário Go (Windows x64) ──────────────────────────────
FROM golang:1.22-alpine AS go-builder

RUN apk add --no-cache git
RUN go install github.com/tc-hib/go-winres@latest

WORKDIR /src
COPY go-base/ .

RUN go-winres make && \
    CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -o base.exe .

# ── Stage 2: Compila o JAR Spring Boot ───────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS java-builder

WORKDIR /app

# Baixa dependências antes de copiar o código-fonte (melhor uso de cache)
COPY backend/pom.xml .
RUN mvn dependency:go-offline -q

# Copia o base.exe compilado para dentro dos resources antes do build
COPY --from=go-builder /src/base.exe ./src/main/resources/base.exe

COPY backend/src ./src
RUN mvn package -DskipTests -q

# ── Stage 3: Imagem de runtime mínima ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=java-builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
