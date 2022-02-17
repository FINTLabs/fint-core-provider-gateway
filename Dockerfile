FROM gradle:7.2-jdk11-openj9 as builder
USER root
COPY . .
RUN gradle --no-daemon build

FROM gcr.io/distroless/java
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/libs/fint-provider-v2-*.jar /data/app.jar
CMD ["/data/app.jar"]
