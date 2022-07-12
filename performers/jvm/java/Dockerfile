# syntax = edrevo/dockerfile-plus

# A Docker image for the Couchbase SDK performance test performer for Java.
# Build from root with:
# docker build -f performers/jvm/java/Dockerfile .

INCLUDE+ performers/jvm/core/Dockerfile.base

COPY --from=0 /app/performers/jvm/java/target/performer-java-1.0-SNAPSHOT-jar-with-dependencies.jar .
ENTRYPOINT ["java", "-jar", "performer-java-1.0-SNAPSHOT-jar-with-dependencies.jar"]