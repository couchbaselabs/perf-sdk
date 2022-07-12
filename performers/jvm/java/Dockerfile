# A Docker image for the Couchbase SDK performance test performer for Java.
# Build from root with:
# docker build -f performers/jvm/java/Dockerfile .

FROM maven:3.8.1-openjdk-17
WORKDIR /app

# Could improve this a lot, only need the GRPC and performers/jvm folders
COPY . .

WORKDIR /app/performers/jvm
RUN mvn package

# Multistage build to keep things small
FROM maven:3.8.1-openjdk-17

WORKDIR /app

COPY --from=0 /app/performers/jvm/java/target/performer-java-1.0-SNAPSHOT-jar-with-dependencies.jar .
ENTRYPOINT ["java", "-jar", "performer-java-1.0-SNAPSHOT-jar-with-dependencies.jar"]