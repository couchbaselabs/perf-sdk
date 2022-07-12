# syntax = edrevo/dockerfile-plus

# A Docker image for the Couchbase SDK performance test performer for Scala.
# Build from root with:
# docker build -f performers/jvm/scala/Dockerfile .

INCLUDE+ performers/jvm/core/Dockerfile.base

COPY --from=0 /app/performers/jvm/scala/target/performer-scala-1.0-SNAPSHOT-jar-with-dependencies.jar .
ENTRYPOINT ["java", "-jar", "performer-scala-1.0-SNAPSHOT-jar-with-dependencies.jar"]