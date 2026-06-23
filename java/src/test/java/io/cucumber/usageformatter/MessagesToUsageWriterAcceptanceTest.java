package io.cucumber.usageformatter;

import io.cucumber.messages.NdjsonToMessageReader;
import io.cucumber.messages.ndjson.Json;
import io.cucumber.messages.types.Envelope;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static tools.jackson.core.util.DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
import static tools.jackson.core.util.Separators.Spacing.AFTER;

class MessagesToUsageWriterAcceptanceTest {
    private static final NdjsonToMessageReader.Deserializer deserializer = Json.instance()
            .map(json -> json.deserializer(Envelope.class))
            .orElseThrow()::readValue;

    private static final MessagesToUsageWriter.Serializer serializer = JsonMapper.builder()
            .changeDefaultPropertyInclusion(value -> value
                    .withContentInclusion(NON_ABSENT)
                    .withValueInclusion(NON_ABSENT)
            )
            .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
            .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
            .defaultPrettyPrinter(new DefaultPrettyPrinter(
                    Separators.createDefaultInstance()
                            .withObjectNameValueSpacing(AFTER)
            )
                    .withArrayIndenter(SYSTEM_LINEFEED_INSTANCE)
                    .withObjectIndenter(SYSTEM_LINEFEED_INSTANCE)
            )
            .build()
            .writerWithDefaultPrettyPrinter()::writeValue;

    static List<TestCase> acceptance() {
        Map<String, MessagesToUsageWriter.Builder> formats = new LinkedHashMap<>();
        formats.put("json", MessagesToUsageWriter.builder(serializer));
        formats.put("unused.txt", MessagesToUsageWriter.builder(new UnusedReportSerializer()));
        formats.put("step-definitions.txt", MessagesToUsageWriter.builder(UsageReportSerializer.builder().build()));
        formats.put("with-steps.txt", MessagesToUsageWriter.builder(UsageReportSerializer.builder()
                .feature(UsageReportSerializer.PlainTextFeature.INCLUDE_STEPS, true)
                .maxStepsPerStepDefinition(5)
                .build()));

        List<Path> sources = getSources();

        List<TestCase> testCases = new ArrayList<>();
        sources.forEach(path ->
                formats.forEach((formatName, format) ->
                        testCases.add(new TestCase(path, formatName, format))));

        return testCases;
    }

    private static List<Path> getSources() {
        return Arrays.asList(
                Paths.get("../testdata/src/ambiguous.ndjson"),
                Paths.get("../testdata/src/minimal.ndjson"),
                Paths.get("../testdata/src/unused-steps.ndjson"),
                Paths.get("../testdata/src/multiple-features.ndjson")
        );
    }

    private static <T extends OutputStream> T writeUsageReport(TestCase testCase, T out, MessagesToUsageWriter.Builder builder) throws IOException {
        try (InputStream in = Files.newInputStream(testCase.source)) {
            try (NdjsonToMessageReader reader = new NdjsonToMessageReader(in, deserializer)) {
                try (MessagesToUsageWriter writer = builder.build(out)) {
                    for (Envelope envelope : reader.lines().toList()) {
                        writer.write(envelope);
                    }
                }
            }
        }
        return out;
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    void test(TestCase testCase) throws IOException {
        ByteArrayOutputStream bytes = writeUsageReport(testCase, new ByteArrayOutputStream(), testCase.builder);
        assertThat(bytes.toString(UTF_8)).isEqualToIgnoringNewLines(Files.readString(testCase.expected));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    @Disabled
    void updateExpectedFiles(TestCase testCase) throws IOException {
        try (OutputStream out = Files.newOutputStream(testCase.expected)) {
            writeUsageReport(testCase, out, testCase.builder);
            if (!testCase.format.equals("json")) {
                // Render output in console, easier to inspect results
                Files.copy(testCase.expected, System.out);
            }
        }
    }

    static class TestCase {
        private final Path source;
        private final String format;
        private final MessagesToUsageWriter.Builder builder;
        private final Path expected;

        private final String name;

        TestCase(Path source, String format, MessagesToUsageWriter.Builder builder) {
            this.source = source;
            this.format = format;
            this.builder = builder;
            String fileName = source.getFileName().toString();
            this.name = fileName.substring(0, fileName.lastIndexOf(".ndjson"));
            this.expected = requireNonNull(source.getParent()).resolve(name + "." + format);
        }

        @Override
        public String toString() {
            return name + " -> " + format;
        }

    }

}

