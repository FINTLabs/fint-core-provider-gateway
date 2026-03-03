package no.fintlabs.provider.performance;

import com.sun.management.OperatingSystemMXBean;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.util.unit.DataSize;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class KafkaProducerBenchmark {

    private static final double KAFKA_DEFAULT_BATCH_SIZE_BYTES = 16 * 1024d;
    private static final String DEFAULT_SCENARIOS =
            "default;" +
                    "linger=0ms,batch=16KB;" +
                    "linger=1ms,batch=16KB;" +
                    "linger=5ms,batch=32KB;" +
                    "linger=10ms,batch=64KB;" +
                    "linger=20ms,batch=128KB;" +
                    "linger=50ms,batch=256KB;" +
                    "linger=75ms,batch=256KB;" +
                    "linger=100ms,batch=512KB";

    public static void main(String[] args) throws Exception {
        BenchmarkConfig config = BenchmarkConfig.fromSystemProperties();

        System.out.println("Kafka producer benchmark");
        System.out.println("Started: " + Instant.now());
        System.out.println("Bootstrap servers: " + config.bootstrapServers());
        System.out.println("Messages per scenario: " + config.messageCount());
        System.out.println("Warmup messages: " + config.warmupMessageCount());
        System.out.println("Payload size: " + config.payloadSize());
        if (!config.simulatedExtraRttPerBatch().isZero()) {
            System.out.println("Modeled extra RTT per batch: " + config.simulatedExtraRttPerBatch());
        }
        System.out.println("Scenarios: " + config.scenarios().size());
        for (Scenario scenario : config.scenarios()) {
            System.out.println(" - " + scenario.name());
        }
        System.out.println();

        waitForKafkaReady(config);

        List<ScenarioResult> results = new ArrayList<>();
        for (Scenario scenario : config.scenarios()) {
            results.add(runScenario(config, scenario));
        }

        printSummary(results, !config.simulatedExtraRttPerBatch().isZero());
    }

    private static ScenarioResult runScenario(BenchmarkConfig config, Scenario scenario) throws Exception {
        String topic = buildTopicName(config.topicPrefix(), scenario.name());
        ensureTopic(config, topic);

        byte[] payload = createPayload(config.payloadSize());
        ProducerRunConfig producerRunConfig = ProducerRunConfig.from(config, scenario);

        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerRunConfig.properties())) {
            if (config.warmupMessageCount() > 0) {
                produceMessages(
                        producer,
                        topic,
                        payload,
                        config.warmupMessageCount(),
                        config.maxInFlightRequests(),
                        config.timeout()
                );
            }

            MemorySampler memorySampler = new MemorySampler(Duration.ofMillis(100));
            memorySampler.start();
            long startCpuNanos = processCpuTimeNanos().orElse(0L);
            long startNanos = System.nanoTime();

            ProduceSummary summary = produceMessages(
                    producer,
                    topic,
                    payload,
                    config.messageCount(),
                    config.maxInFlightRequests(),
                    config.timeout()
            );

            long durationNanos = System.nanoTime() - startNanos;
            long cpuDeltaNanos = processCpuTimeNanos().map(cpu -> cpu - startCpuNanos).orElse(-1L);
            long peakHeapBytes = memorySampler.stopAndGetPeakHeapBytes();

            Map<MetricName, ? extends Metric> metrics = producer.metrics();

            ScenarioResult result = new ScenarioResult(
                    scenario.name(),
                    config.messageCount(),
                    config.messageCount() * payload.length,
                    durationNanos,
                    cpuDeltaNanos,
                    peakHeapBytes,
                    summary.errors(),
                    metric(metrics, "record-send-rate"),
                    metric(metrics, "request-latency-avg"),
                    metric(metrics, "batch-size-avg"),
                    scenario.batchSize() == null ? KAFKA_DEFAULT_BATCH_SIZE_BYTES : scenario.batchSize().toBytes(),
                    config.simulatedExtraRttPerBatch()
            );

            printScenarioResult(result);
            return result;
        }
    }

    private static ProduceSummary produceMessages(
            KafkaProducer<String, byte[]> producer,
            String topic,
            byte[] payload,
            long messageCount,
            int maxInFlightRequests,
            Duration timeout
    ) throws InterruptedException, TimeoutException {
        if (messageCount <= 0) {
            return new ProduceSummary(0);
        }
        if (messageCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("messageCount is too high for this benchmark runner: " + messageCount);
        }

        Semaphore inFlight = new Semaphore(Math.max(1, maxInFlightRequests));
        CountDownLatch latch = new CountDownLatch((int) messageCount);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        long progressStep = Math.max(1, messageCount / 10);
        for (long i = 0; i < messageCount; i++) {
            inFlight.acquire();
            String key = "key-" + (i % 1024);
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, payload);
            producer.send(record, (metadata, exception) -> {
                try {
                    if (exception != null) {
                        errors.incrementAndGet();
                        firstError.compareAndSet(null, exception);
                    }
                } finally {
                    latch.countDown();
                    inFlight.release();
                }
            });

            if ((i + 1) % progressStep == 0 || i + 1 == messageCount) {
                System.out.println("  sent " + (i + 1) + "/" + messageCount + " messages");
            }
        }

        producer.flush();
        boolean completed = latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new TimeoutException("Timed out while waiting for message acknowledgements");
        }

        Throwable throwable = firstError.get();
        if (throwable != null) {
            throw new RuntimeException("Producer callback returned errors", throwable);
        }

        return new ProduceSummary(errors.get());
    }

    private static void ensureTopic(BenchmarkConfig config, String topicName) throws Exception {
        Map<String, Object> adminConfig = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers()
        );
        try (AdminClient adminClient = AdminClient.create(adminConfig)) {
            KafkaFuture<Map<String, TopicListing>> listingsFuture = adminClient.listTopics().namesToListings();
            Map<String, TopicListing> listings = listingsFuture.get(30, TimeUnit.SECONDS);
            if (listings.containsKey(topicName)) {
                return;
            }

            NewTopic topic = new NewTopic(topicName, config.partitions(), (short) 1);
            adminClient.createTopics(List.of(topic)).all().get(30, TimeUnit.SECONDS);
        }
    }

    private static void waitForKafkaReady(BenchmarkConfig config) throws Exception {
        long deadlineNanos = System.nanoTime() + config.startupTimeout().toNanos();
        int attempts = 0;
        while (System.nanoTime() < deadlineNanos) {
            attempts++;
            try (AdminClient adminClient = AdminClient.create(Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers(),
                    AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000",
                    AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000"
            ))) {
                adminClient.describeCluster().clusterId().get(5, TimeUnit.SECONDS);
                System.out.println("Kafka is reachable after " + attempts + " attempt(s)");
                return;
            } catch (Exception ignored) {
                Thread.sleep(1000);
            }
        }

        throw new IllegalStateException(
                "Kafka broker at %s was not reachable within %s. " +
                        "Check `docker compose ps` and `docker compose logs kafka`."
                        .formatted(config.bootstrapServers(), config.startupTimeout())
        );
    }

    private static String buildTopicName(String prefix, String scenarioName) {
        String sanitizedScenario = scenarioName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String topic = "%s-%s-%d".formatted(prefix, sanitizedScenario, System.currentTimeMillis());
        return topic.length() <= 249 ? topic : topic.substring(0, 249);
    }

    private static byte[] createPayload(DataSize payloadSize) {
        int payloadBytes = Math.toIntExact(payloadSize.toBytes());
        byte[] payload = new byte[payloadBytes];
        Arrays.fill(payload, (byte) 'x');
        return payload;
    }

    private static Optional<Long> processCpuTimeNanos() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        if (osBean == null) {
            return Optional.empty();
        }
        return Optional.of(osBean.getProcessCpuTime());
    }

    private static double cpuPercent(long cpuDeltaNanos, long durationNanos) {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        if (osBean == null || cpuDeltaNanos < 0 || durationNanos <= 0) {
            return Double.NaN;
        }
        return (cpuDeltaNanos / (double) durationNanos) * 100.0 / osBean.getAvailableProcessors();
    }

    private static double metric(Map<MetricName, ? extends Metric> metrics, String metricName) {
        return metrics.entrySet().stream()
                .filter(entry -> entry.getKey().name().equals(metricName))
                .map(entry -> entry.getValue().metricValue())
                .filter(Objects::nonNull)
                .mapToDouble(value -> ((Number) value).doubleValue())
                .filter(value -> !Double.isNaN(value) && !Double.isInfinite(value))
                .findFirst()
                .orElse(Double.NaN);
    }

    private static void printScenarioResult(ScenarioResult result) {
        System.out.println();
        System.out.println("Scenario: " + result.scenarioName());
        System.out.println("  Messages: " + result.messageCount());
        System.out.println("  Total time: " + formatDurationNanos(result.durationNanos()));
        System.out.printf(Locale.ROOT, "  Avg per message: %.3f ms%n", result.avgMillisPerMessage());
        System.out.printf(Locale.ROOT, "  Throughput: %.2f msg/s%n", result.messagesPerSecond());
        System.out.printf(Locale.ROOT, "  Producer metric record-send-rate: %.2f msg/s%n", result.recordSendRate());
        System.out.printf(Locale.ROOT, "  Producer metric request-latency-avg: %.3f ms%n", result.requestLatencyAvgMs());
        System.out.printf(Locale.ROOT, "  Producer metric batch-size-avg: %.0f bytes%n", result.batchSizeAvgBytes());
        System.out.printf(Locale.ROOT, "  Process CPU usage (avg): %.2f%%%n", result.cpuPercent());
        System.out.println("  Peak heap used: " + DataSize.ofBytes(result.peakHeapBytes()));
        if (result.simulationEnabled()) {
            System.out.printf(Locale.ROOT, "  Modeled extra time: %.3f s%n", result.modeledExtraSeconds());
            System.out.println("  Modeled total time: " + formatDurationNanos(result.modeledDurationNanos()));
            System.out.printf(Locale.ROOT, "  Modeled throughput: %.2f msg/s%n", result.modeledMessagesPerSecond());
            System.out.printf(Locale.ROOT, "  Modeled avg per message: %.3f ms%n", result.modeledAvgMillisPerMessage());
        }
        System.out.println("  Errors: " + result.errors());
    }

    private static void printSummary(List<ScenarioResult> results, boolean simulationEnabled) {
        if (results.isEmpty()) {
            return;
        }

        ScenarioResult baseline = findBaseline(results);
        String markdown = buildMarkdownSummary(results, baseline, simulationEnabled);

        System.out.println();
        System.out.println("Summary (Markdown)");
        System.out.println("Baseline scenario: " + baseline.scenarioName());
        System.out.println();
        System.out.println(markdown);

        writeMarkdownSummary(markdown);
    }

    private static ScenarioResult findBaseline(List<ScenarioResult> results) {
        return results.stream()
                .filter(result -> "default".equalsIgnoreCase(result.scenarioName()))
                .findFirst()
                .orElse(results.get(0));
    }

    private static String buildMarkdownSummary(
            List<ScenarioResult> results,
            ScenarioResult baseline,
            boolean simulationEnabled
    ) {
        StringBuilder table = new StringBuilder();
        if (simulationEnabled) {
            table.append("| Scenario | Total Time | Msg/s | Delta vs baseline | CPU% | Req latency ms | Batch avg bytes | Modeled Total | Modeled Msg/s | Modeled delta | Errors |\n");
            table.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        } else {
            table.append("| Scenario | Total Time | Msg/s | Delta vs baseline | CPU% | Req latency ms | Batch avg bytes | Errors |\n");
            table.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        }

        double baselineMsgPerSecond = baseline.messagesPerSecond();
        double baselineModeledMsgPerSecond = baseline.modeledMessagesPerSecond();

        for (ScenarioResult result : results) {
            if (simulationEnabled) {
                table.append("| ")
                        .append(result.scenarioName()).append(" | ")
                        .append(formatDurationNanos(result.durationNanos())).append(" | ")
                        .append(formatDecimal(result.messagesPerSecond(), 2)).append(" | ")
                        .append(formatPercentageDelta(result.messagesPerSecond(), baselineMsgPerSecond)).append(" | ")
                        .append(formatDecimal(result.cpuPercent(), 2)).append(" | ")
                        .append(formatDecimal(result.requestLatencyAvgMs(), 3)).append(" | ")
                        .append(formatDecimal(result.batchSizeAvgBytes(), 0)).append(" | ")
                        .append(formatDurationNanos(result.modeledDurationNanos())).append(" | ")
                        .append(formatDecimal(result.modeledMessagesPerSecond(), 2)).append(" | ")
                        .append(formatPercentageDelta(result.modeledMessagesPerSecond(), baselineModeledMsgPerSecond)).append(" | ")
                        .append(result.errors()).append(" |\n");
            } else {
                table.append("| ")
                        .append(result.scenarioName()).append(" | ")
                        .append(formatDurationNanos(result.durationNanos())).append(" | ")
                        .append(formatDecimal(result.messagesPerSecond(), 2)).append(" | ")
                        .append(formatPercentageDelta(result.messagesPerSecond(), baselineMsgPerSecond)).append(" | ")
                        .append(formatDecimal(result.cpuPercent(), 2)).append(" | ")
                        .append(formatDecimal(result.requestLatencyAvgMs(), 3)).append(" | ")
                        .append(formatDecimal(result.batchSizeAvgBytes(), 0)).append(" | ")
                        .append(result.errors()).append(" |\n");
            }
        }

        return table.toString();
    }

    private static String formatDecimal(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private static String formatPercentageDelta(double value, double baseline) {
        if (Double.isNaN(value) || Double.isInfinite(value) || Double.isNaN(baseline) || baseline == 0) {
            return "n/a";
        }
        double delta = ((value - baseline) / baseline) * 100.0;
        return String.format(Locale.ROOT, "%+.2f%%", delta);
    }

    private static void writeMarkdownSummary(String markdown) {
        Path reportPath = Path.of("build", "reports", "kafka-producer-benchmark-summary-" + System.currentTimeMillis() + ".md");
        try {
            Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, markdown, StandardCharsets.UTF_8);
            System.out.println("Saved summary report: " + reportPath.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Could not write summary report: " + e.getMessage());
        }
    }

    private static String formatDurationNanos(long durationNanos) {
        Duration duration = Duration.ofNanos(durationNanos);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).toSeconds();
        long millis = duration.minusMinutes(minutes).minusSeconds(seconds).toMillis();
        return "%dm %ds %dms".formatted(minutes, seconds, millis);
    }

    private record ProduceSummary(int errors) {
    }

    private record Scenario(
            String name,
            Duration linger,
            DataSize batchSize
    ) {
        private static List<Scenario> parse(String rawValue) {
            List<Scenario> scenarios = new ArrayList<>();
            for (String token : rawValue.split(";")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if ("default".equalsIgnoreCase(trimmed)) {
                    scenarios.add(new Scenario("default", null, null));
                    continue;
                }

                Map<String, String> values = new LinkedHashMap<>();
                for (String part : trimmed.split(",")) {
                    String[] keyValue = part.trim().split("=", 2);
                    if (keyValue.length != 2) {
                        throw new IllegalArgumentException("Invalid scenario element: " + part);
                    }
                    values.put(keyValue[0].trim().toLowerCase(Locale.ROOT), keyValue[1].trim());
                }

                Duration linger = values.containsKey("linger")
                        ? DurationStyle.detectAndParse(values.get("linger"))
                        : null;
                DataSize batch = values.containsKey("batch")
                        ? DataSize.parse(values.get("batch"))
                        : null;
                String name = values.getOrDefault("name", buildName(linger, batch));

                scenarios.add(new Scenario(name, linger, batch));
            }

            if (scenarios.isEmpty()) {
                throw new IllegalArgumentException("At least one scenario is required");
            }
            return scenarios;
        }

        private static String buildName(Duration linger, DataSize batchSize) {
            String lingerPart = linger == null ? "default-linger" : "linger-" + linger.toMillis() + "ms";
            String batchPart = batchSize == null ? "default-batch" : "batch-" + batchSize.toBytes() + "b";
            return lingerPart + "-" + batchPart;
        }
    }

    private record ScenarioResult(
            String scenarioName,
            long messageCount,
            long totalPayloadBytes,
            long durationNanos,
            long cpuDeltaNanos,
            long peakHeapBytes,
            int errors,
            double recordSendRate,
            double requestLatencyAvgMs,
            double batchSizeAvgBytes,
            double configuredBatchSizeBytes,
            Duration simulatedExtraRttPerBatch
    ) {
        private double messagesPerSecond() {
            if (durationNanos <= 0) {
                return Double.NaN;
            }
            return messageCount / (durationNanos / 1_000_000_000.0);
        }

        private double avgMillisPerMessage() {
            if (messageCount <= 0) {
                return Double.NaN;
            }
            return durationNanos / 1_000_000.0 / messageCount;
        }

        private double cpuPercent() {
            return KafkaProducerBenchmark.cpuPercent(cpuDeltaNanos, durationNanos);
        }

        private boolean simulationEnabled() {
            return simulatedExtraRttPerBatch != null && !simulatedExtraRttPerBatch.isZero();
        }

        private double modeledBatchSizeBytes() {
            if (!Double.isNaN(batchSizeAvgBytes) && !Double.isInfinite(batchSizeAvgBytes) && batchSizeAvgBytes > 0) {
                return batchSizeAvgBytes;
            }
            if (!Double.isNaN(configuredBatchSizeBytes) && configuredBatchSizeBytes > 0) {
                return configuredBatchSizeBytes;
            }
            return KAFKA_DEFAULT_BATCH_SIZE_BYTES;
        }

        private double estimatedBatchCount() {
            return totalPayloadBytes / modeledBatchSizeBytes();
        }

        private long modeledDurationNanos() {
            if (!simulationEnabled()) {
                return durationNanos;
            }
            return durationNanos + (long) (estimatedBatchCount() * simulatedExtraRttPerBatch.toNanos());
        }

        private double modeledExtraSeconds() {
            if (!simulationEnabled()) {
                return 0.0;
            }
            return estimatedBatchCount() * simulatedExtraRttPerBatch.toNanos() / 1_000_000_000.0;
        }

        private double modeledMessagesPerSecond() {
            long modeledNanos = modeledDurationNanos();
            if (modeledNanos <= 0) {
                return Double.NaN;
            }
            return messageCount / (modeledNanos / 1_000_000_000.0);
        }

        private double modeledAvgMillisPerMessage() {
            if (messageCount <= 0) {
                return Double.NaN;
            }
            return modeledDurationNanos() / 1_000_000.0 / messageCount;
        }
    }

    private record ProducerRunConfig(Properties properties) {
        private static ProducerRunConfig from(BenchmarkConfig config, Scenario scenario) {
            Properties properties = new Properties();
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
            properties.put(ProducerConfig.CLIENT_ID_CONFIG, "kafka-producer-benchmark-" + scenario.name());
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
            properties.put(ProducerConfig.ACKS_CONFIG, config.acks());
            properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, config.compressionType());

            if (scenario.linger() != null) {
                properties.put(ProducerConfig.LINGER_MS_CONFIG, scenario.linger().toMillis());
            }
            if (scenario.batchSize() != null) {
                long batchBytes = scenario.batchSize().toBytes();
                if (batchBytes > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("batch-size is too large: " + scenario.batchSize());
                }
                properties.put(ProducerConfig.BATCH_SIZE_CONFIG, (int) batchBytes);
            }

            return new ProducerRunConfig(properties);
        }
    }

    private record BenchmarkConfig(
            String bootstrapServers,
            long messageCount,
            long warmupMessageCount,
            DataSize payloadSize,
            String acks,
            String compressionType,
            Duration timeout,
            Duration startupTimeout,
            Duration simulatedExtraRttPerBatch,
            int partitions,
            int maxInFlightRequests,
            String topicPrefix,
            List<Scenario> scenarios
    ) {
        private static BenchmarkConfig fromSystemProperties() {
            String bootstrapServers = property("benchmark.bootstrapServers", "localhost:9092");
            long messageCount = Long.parseLong(property("benchmark.messages", "200000"));
            long warmupMessageCount = Long.parseLong(property("benchmark.warmupMessages", "20000"));
            DataSize payloadSize = DataSize.parse(property("benchmark.payloadSize", "1KB"));
            String acks = property("benchmark.acks", "all");
            String compressionType = property("benchmark.compressionType", "none");
            Duration timeout = DurationStyle.detectAndParse(property("benchmark.timeout", "10m"));
            Duration startupTimeout = DurationStyle.detectAndParse(property("benchmark.startupTimeout", "90s"));
            Duration simulatedExtraRttPerBatch = DurationStyle.detectAndParse(property("benchmark.simulatedExtraRttPerBatch", "0ms"));
            int partitions = Integer.parseInt(property("benchmark.partitions", "12"));
            int maxInFlightRequests = Integer.parseInt(property("benchmark.maxInFlight", "20000"));
            String topicPrefix = property("benchmark.topicPrefix", "provider-benchmark");
            String scenarioProperty = property("benchmark.scenarios", DEFAULT_SCENARIOS);
            List<Scenario> scenarios = Scenario.parse(scenarioProperty);

            return new BenchmarkConfig(
                    bootstrapServers,
                    messageCount,
                    warmupMessageCount,
                    payloadSize,
                    acks,
                    compressionType,
                    timeout,
                    startupTimeout,
                    simulatedExtraRttPerBatch,
                    partitions,
                    maxInFlightRequests,
                    topicPrefix,
                    scenarios
            );
        }
    }

    private static String property(String key, String defaultValue) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }
        String envKey = key
                .toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
        String environmentValue = System.getenv(envKey);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return defaultValue;
    }

    private static final class MemorySampler {
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        private final AtomicLong peakHeapBytes = new AtomicLong(0);
        private final Duration interval;

        private MemorySampler(Duration interval) {
            this.interval = interval;
        }

        private void start() {
            sample();
            scheduler.scheduleAtFixedRate(
                    this::sample,
                    interval.toMillis(),
                    interval.toMillis(),
                    TimeUnit.MILLISECONDS
            );
        }

        private long stopAndGetPeakHeapBytes() throws InterruptedException {
            scheduler.shutdownNow();
            scheduler.awaitTermination(1, TimeUnit.SECONDS);
            return peakHeapBytes.get();
        }

        private void sample() {
            long used = memoryMXBean.getHeapMemoryUsage().getUsed();
            peakHeapBytes.accumulateAndGet(used, Math::max);
        }
    }
}
