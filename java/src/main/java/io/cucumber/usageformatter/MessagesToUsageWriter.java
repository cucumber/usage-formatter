package io.cucumber.usageformatter;

import io.cucumber.messages.types.Envelope;
import io.cucumber.query.Query;
import io.cucumber.query.Repository;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static io.cucumber.query.Repository.RepositoryFeature.INCLUDE_GHERKIN_DOCUMENTS;
import static io.cucumber.query.Repository.RepositoryFeature.INCLUDE_STEP_DEFINITIONS;
import static java.util.Objects.requireNonNull;

/**
 * Creates a usage report for step definitions based on a test run.
 * <p>
 * Note: Messages are first collected and only written once the stream is
 * closed.
 */
public final class MessagesToUsageWriter implements AutoCloseable {

    private final OutputStreamWriter out;
    private final Repository repository = Repository.builder()
            .feature(INCLUDE_GHERKIN_DOCUMENTS, true)
            .feature(INCLUDE_STEP_DEFINITIONS, true)
            .build();
    private final Query query = new Query(repository);
    private final Serializer serializer;
    private final Function<String, String> uriFormatter;
    private boolean streamClosed = false;

    MessagesToUsageWriter(OutputStream out, Serializer serializer, Function<String, String> uriFormatter) {
        this.out = new OutputStreamWriter(
                requireNonNull(out),
                StandardCharsets.UTF_8);
        this.serializer = requireNonNull(serializer);
        this.uriFormatter = requireNonNull(uriFormatter);
    }

    public void write(Envelope envelope) throws IOException {
        if (streamClosed) {
            throw new IOException("Stream closed");
        }
        repository.update(envelope);
    }

    public static Builder builder(Serializer serializer) {
        return new Builder(serializer);
    }

    public static final class Builder {
        private final Serializer serializer;
        private Function<String, String> uriFormatter = Function.identity();

        private Builder(Serializer serializer) {
            this.serializer = requireNonNull(serializer);
        }

        private static Function<String, String> removePrefix(String prefix) {
            // TODO: Needs coverage
            return s -> {
                if (s.startsWith(prefix)) {
                    return s.substring(prefix.length());
                }
                return s;
            };
        }

        /**
         * Removes a given prefix from all URI locations.
         * <p>
         * The typical usage would be to trim the current working directory.
         * This makes the report more readable.
         */
        public Builder removeUriPrefix(String prefix) {
            // TODO: Needs coverage
            this.uriFormatter = removePrefix(requireNonNull(prefix));
            return this;
        }

        public MessagesToUsageWriter build(OutputStream out) {
            requireNonNull(out);
            return new MessagesToUsageWriter(out, serializer, uriFormatter);
        }
    }

    @Override
    public void close() throws IOException {
        if (streamClosed) {
            return;
        }
        try {
            UsageReport report = new UsageReportBuilder(query, uriFormatter).build();
            serializer.writeValue(out, report);
        } finally {
            try {
                out.close();
            } finally {
                streamClosed = true;
            }
        }
    }

    @FunctionalInterface
    public interface Serializer {

        void writeValue(Writer writer, UsageReport value) throws IOException;

    }
}
