package io.cucumber.usageformatter;

import io.cucumber.messages.NdjsonToMessageIterable;
import io.cucumber.messages.types.Envelope;
import io.cucumber.usageformatter.MessagesToUsageWriter.Builder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

import static io.cucumber.usageformatter.Jackson.OBJECT_MAPPER;
import static io.cucumber.usageformatter.Jackson.PRETTY_PRINTER;
import static io.cucumber.usageformatter.MessagesToUsageWriter.builder;
import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;

class MessagesToUsageWriterAcceptanceTest {
    private static final NdjsonToMessageIterable.Deserializer deserializer = (json) -> OBJECT_MAPPER.readValue(json, Envelope.class);
    private static final MessagesToUsageWriter.Serializer jsonSerializer = OBJECT_MAPPER.writer(PRETTY_PRINTER)::writeValue;
    private static final MessagesToUsageWriter.Serializer plainTextSerializer = new PlainTextSerializer();

    static List<TestCase> acceptance() throws IOException {
        Map<String, Builder> formats = new LinkedHashMap<>();
        formats.put("json", builder(jsonSerializer));
        formats.put("plain.txt", builder(plainTextSerializer));
        ;

        List<Path> sources = getSources();

        List<TestCase> testCases = new ArrayList<>();
        sources.forEach(path ->
                formats.forEach((formatName, format) ->
                        testCases.add(new TestCase(path, formatName, format))));

        return testCases;
    }

    private static List<Path> getSources() {
        return Arrays.asList(
                Paths.get("../testdata/src/minimal.ndjson"),
                Paths.get("../testdata/src/unused-steps.ndjson"),
                Paths.get("../testdata/src/multiple-features.ndjson")
        );

    }

    private static <T extends OutputStream> T writePrettyReport(TestCase testCase, T out, Builder builder) throws IOException {
        try (InputStream in = Files.newInputStream(testCase.source)) {
            try (NdjsonToMessageIterable envelopes = new NdjsonToMessageIterable(in, deserializer)) {
                try (MessagesToUsageWriter writer = builder.build(out)) {
                    for (Envelope envelope : envelopes) {
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
        ByteArrayOutputStream bytes = writePrettyReport(testCase, new ByteArrayOutputStream(), testCase.builder);
        assertThat(bytes.toString()).isEqualToIgnoringNewLines(new String(readAllBytes(testCase.expected)));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    @Disabled
    void updateExpectedFiles(TestCase testCase) throws IOException {
        try (OutputStream out = Files.newOutputStream(testCase.expected)) {
            writePrettyReport(testCase, out, testCase.builder);
            if (!testCase.format.equals("json")) {
                // Render output in console, easier to inspect results
                Files.copy(testCase.expected, System.out);
            }
        }
    }

    static class TestCase {
        private final Path source;
        private final String format;
        private final Builder builder;
        private final Path expected;

        private final String name;

        TestCase(Path source, String format, Builder builder) {
            this.source = source;
            this.format = format;
            this.builder = builder;
            String fileName = source.getFileName().toString();
            this.name = fileName.substring(0, fileName.lastIndexOf(".ndjson"));
            this.expected = source.getParent().resolve(name + "." + format);
        }

        @Override
        public String toString() {
            return name + " -> " + format;
        }

    }

}

