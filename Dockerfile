# Cwtch Messenger - Multi-stage Docker Build
# Creates a minimal container for running the terminal messenger

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy protocol library first (for caching)
COPY cwtch-java-protocol/ ./cwtch-java-protocol/
RUN cd cwtch-java-protocol && mvn install -DskipTests -q

# Copy messenger source
COPY CwtchMessenger/ ./CwtchMessenger/

# Build the application
RUN cd CwtchMessenger && mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Install Tor (optional)
RUN apk add --no-cache tor

# Create non-root user for security
RUN addgroup -S cwtch && adduser -S cwtch -G cwtch
USER cwtch

WORKDIR /app

# Copy the built JAR
COPY --from=builder /build/CwtchMessenger/target/cwtch-terminal.jar ./cwtch-terminal.jar

# Create data directory
RUN mkdir -p /home/cwtch/.cwtch

# Expose port for hidden service
EXPOSE 9878

# Default to offline mode in container
ENTRYPOINT ["java", "-jar", "cwtch-terminal.jar"]
CMD ["--offline"]
