FROM gradle:9.4.0-jdk25 AS builder
USER root
COPY . .
RUN ./gradlew --no-daemon build

FROM gcr.io/distroless/java25-debian13
ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"
COPY --from=builder /home/gradle/build/libs/fint-core-provider-gateway-*.jar /data/app.jar
CMD ["/data/app.jar"]
