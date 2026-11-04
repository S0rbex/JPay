FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre
RUN groupadd --system jpay \
    && useradd --system --gid jpay --no-create-home --shell /usr/sbin/nologin jpay \
    && mkdir -p /app/logs \
    && chown -R jpay:jpay /app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER jpay
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
