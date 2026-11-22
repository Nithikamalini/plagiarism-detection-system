# Multi-stage Dockerfile for Plagiarism Detection System
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy dependency libraries and sources
COPY lib/ lib/
COPY src/ src/

# Compile Java classes
RUN mkdir -p bin && \
    javac --release 17 -encoding UTF-8 -cp "lib/*" -d bin $(find src/main/java -name "*.java")

# Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy compiled classes, libraries, and web assets
COPY --from=builder /app/bin bin/
COPY lib/ lib/
COPY src/main/resources/ src/main/resources/

EXPOSE 8080

ENV PORT=8080
ENV mongodb.uri=mongodb://mongo:27017/plagiarism_db

ENTRYPOINT ["java", "-cp", "bin:lib/*", "com.plagiarism.PlagiarismApplication", "8080"]
