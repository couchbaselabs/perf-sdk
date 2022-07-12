# syntax = edrevo/dockerfile-plus

# A Docker image for the Couchbase SDK performance test performer for Kotlin.
# Build from root with:
# docker build -f performers/jvm/kotlin/Dockerfile .

INCLUDE+ performers/jvm/core/Dockerfile.base

COPY --from=0 /app/performers/jvm/kotlin/target/performer-kotlin-1.0-SNAPSHOT-jar-with-dependencies.jar .
ENTRYPOINT ["java", "-jar", "performer-kotlin-1.0-SNAPSHOT-jar-with-dependencies.jar"]