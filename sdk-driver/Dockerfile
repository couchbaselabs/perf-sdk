# A Docker image for the Couchbase SDK Performance Framework
# Driver

FROM maven:3.8.1-openjdk-17
WORKDIR /app

COPY . .

# Install the GRPC lib to local m2 repo
WORKDIR /app/performers/jvm/grpc
RUN mvn install generate-sources

WORKDIR /app/sdk-driver
RUN mvn package -DskipTests

ENTRYPOINT ["java", "-cp", "/app/sdk-driver/target/performer-driver-1.0-SNAPSHOT.jar", "com.sdk.SdkDriver"]

