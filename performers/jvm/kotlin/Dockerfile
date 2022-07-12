# A Docker image for the Couchbase SDK performance test performer for Kotlin.
# Build from root with:
# docker build -f performers/jvm/kotlin/Dockerfile .

FROM maven:3.8.1-openjdk-17
WORKDIR /app

# Could improve this a lot, only need the GRPC and performers/jvm folders
COPY . .

WORKDIR /app/performers/jvm
RUN mvn package

# Multistage build to keep things small
FROM maven:3.8.1-openjdk-17

WORKDIR /app

COPY --from=0 /app/performers/jvm/kotlin/target/performer-kotlin-1.0-SNAPSHOT-jar-with-dependencies.jar .
ENTRYPOINT ["java", "-jar", "performer-kotlin-1.0-SNAPSHOT-jar-with-dependencies.jar"]