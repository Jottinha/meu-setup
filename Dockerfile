# syntax=docker/dockerfile:1.7

# Stage 1 - Compila o binario Go (Windows x64) com o hmac.key embedded
FROM golang:1.23-alpine AS go-builder

ARG HMAC_KEY

RUN apk add --no-cache git
RUN go install github.com/tc-hib/go-winres@v0.3.3

WORKDIR /src
COPY go-base/ .

RUN test -n "$HMAC_KEY" || (echo "ERRO: HMAC_KEY nao definido (passe via --build-arg)" && exit 1) && \
    printf '%s' "$HMAC_KEY" > ./hmac.key && \
    go-winres make && \
    CGO_ENABLED=0 GOOS=windows GOARCH=amd64 \
        go build -trimpath -ldflags="-s -w" -o base.exe . && \
    rm -f hmac.key

# Stage 2 - Compila o JAR Spring Boot com base.exe + hmac.key nos resources
FROM maven:3.9-eclipse-temurin-21-alpine AS java-builder

ARG HMAC_KEY

WORKDIR /app

COPY backend/pom.xml .
RUN mvn dependency:go-offline -q

COPY --from=go-builder /src/base.exe ./src/main/resources/base.exe

COPY backend/src ./src

RUN test -n "$HMAC_KEY" || (echo "ERRO: HMAC_KEY nao definido (passe via --build-arg)" && exit 1) && \
    printf '%s' "$HMAC_KEY" > ./src/main/resources/hmac.key && \
    mvn package -DskipTests -q

# Stage 3 - Imagem de runtime minima
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=java-builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
