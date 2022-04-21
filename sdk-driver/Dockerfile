# A Docker image for the Couchbase SDK Performance Framework
# Driver

FROM maven:3.8.1-openjdk-17
WORKDIR /app

COPY . .

# Install the GRPC lib to local m2 repo
WORKDIR /app/performers/java/lib/gRPC
RUN mvn install generate-sources

WORKDIR /app/sdk-driver
RUN mvn package -DskipTests

# Multistage build to keep things small
FROM openjdk:17-alpine

WORKDIR /app
COPY --from=0 /app/sdk-driver/target/sdk-driver-1.0-SNAPSHOT.jar .

ENTRYPOINT ["java", "--enable-preview", "-cp", "/app/sdk-driver-1.0-SNAPSHOT.jar", "com.sdk.SdkDriver"]

