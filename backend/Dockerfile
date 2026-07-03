FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy build files first so the dependency download layer is cached
# separately from source — only re-runs when build.gradle.kts changes.
COPY gradlew ./
COPY gradle/ gradle/
RUN chmod +x gradlew
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon -q

COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
