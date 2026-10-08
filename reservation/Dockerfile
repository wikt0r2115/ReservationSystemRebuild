FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY reservation/ ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app app
WORKDIR /app
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]

FROM runtime AS auth
COPY --from=build --chown=app:app /build/auth/target/auth-0.0.1-SNAPSHOT-exec.jar /app/app.jar

FROM runtime AS offer
COPY --from=build --chown=app:app /build/offer/target/offer-0.0.1-SNAPSHOT-exec.jar /app/app.jar

FROM runtime AS availability
COPY --from=build --chown=app:app /build/availability/target/availability-0.0.1-SNAPSHOT-exec.jar /app/app.jar

FROM runtime AS booking
COPY --from=build --chown=app:app /build/booking/target/booking-0.0.1-SNAPSHOT-exec.jar /app/app.jar
