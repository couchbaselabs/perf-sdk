# A Docker image for the Couchbase SDK performance test performer for Java.
# Java Performer

FROM maven:3.8.1-openjdk-17
WORKDIR /app

COPY . .

# Install the GRPC lib to local m2 repo
WORKDIR /app/performers/java/lib/gRPC
RUN mvn install generate-sources

WORKDIR /app/performers/java/
RUN mvn package

# Multistage build to keep things small
FROM maven:3.8.1-openjdk-17

WORKDIR /app
COPY --from=0 /app/performers/java/sdk-performer-java/target/sdk-performer-java-1.0-SNAPSHOT.jar .
ENTRYPOINT ["java", "-jar", "sdk-performer-java-1.0-SNAPSHOT.jar", "com.couchbase.sdk.JavaPerformer"]