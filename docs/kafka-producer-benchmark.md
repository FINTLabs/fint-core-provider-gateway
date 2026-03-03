# Kafka Producer Benchmark

This benchmark is intended to compare producer throughput and latency for different `linger` and `batch-size` settings.

## Why Docker Compose instead of `@EmbeddedKafka`

Use Docker Compose for this benchmark.

`@EmbeddedKafka` is useful for functional tests, but it runs in-process and is not representative for throughput benchmarking. Local Docker gives more realistic producer/network/broker behavior and makes results easier to compare over time.

## Start local Kafka

```bash
docker compose up -d zookeeper kafka
docker compose ps
```

Kafka will be available at `localhost:9092`.

## Run benchmark

```bash
./gradlew benchmarkKafkaProducer
```

Default benchmark config:

- `benchmark.messages=200000`
- `benchmark.warmupMessages=20000`
- `benchmark.payloadSize=1KB`
- `benchmark.startupTimeout=90s`
- `benchmark.simulatedExtraRttPerBatch=0ms`
- `benchmark.scenarios=default;linger=0ms,batch=16KB;linger=1ms,batch=16KB;linger=5ms,batch=32KB;linger=10ms,batch=64KB;linger=20ms,batch=128KB;linger=50ms,batch=256KB;linger=75ms,batch=256KB;linger=100ms,batch=512KB`

## Useful overrides

Run 1 million messages with a custom scenario matrix:

```bash
./gradlew benchmarkKafkaProducer \
  -Pbenchmark.messages=1000000 \
  -Pbenchmark.warmupMessages=50000 \
  -Pbenchmark.payloadSize=2KB \
  -Pbenchmark.simulatedExtraRttPerBatch=10ms \
  -Pbenchmark.scenarios='default;linger=0ms,batch=16KB;linger=5ms,batch=64KB;linger=20ms,batch=128KB;linger=50ms,batch=256KB;linger=100ms,batch=512KB'
```

Alternative with JVM system properties:

```bash
./gradlew benchmarkKafkaProducer \
  -Dbenchmark.messages=1000000 \
  -Dbenchmark.scenarios='default;linger=10ms,batch=64KB;linger=30ms,batch=256KB;linger=100ms,batch=512KB'
```

## Scenario format

- Separator between scenarios: `;`
- Separator between entries in a scenario: `,`
- Supported keys: `linger`, `batch`, `name`

Examples:

- `default`
- `linger=10ms,batch=64KB`
- `name=aggressive,linger=50ms,batch=512KB`

## Metrics printed per scenario

- Total time
- Average milliseconds per message
- Throughput (messages per second)
- Producer metric `record-send-rate`
- Producer metric `request-latency-avg`
- Producer metric `batch-size-avg`
- Approximate process CPU usage
- Peak JVM heap usage
- Final summary table in Markdown format (printed in terminal and written to `build/reports/kafka-producer-benchmark-summary-<timestamp>.md`)

For broker-level CPU/memory, run `docker stats` in parallel while benchmark runs.

To model external-broker network cost without changing the actual send behavior, set `benchmark.simulatedExtraRttPerBatch` (for example `10ms`). The benchmark output will include modeled total time/throughput based on estimated batch count.

If startup fails, verify Kafka status:

```bash
docker compose ps
docker compose logs kafka
```
