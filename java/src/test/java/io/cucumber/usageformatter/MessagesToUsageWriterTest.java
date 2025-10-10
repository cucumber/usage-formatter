package io.cucumber.usageformatter;

import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestRunStarted;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import static io.cucumber.messages.Convertor.toMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessagesToUsageWriterTest {

    @Test
    void it_writes_two_messages_to_usage() throws IOException {
        Instant started = Instant.ofEpochSecond(10);
        Instant finished = Instant.ofEpochSecond(30);

        String out = renderAsSummary(
                Envelope.of(new TestRunStarted(toMessage(started), "some-id")),
                Envelope.of(new TestRunFinished(null, true, toMessage(finished), null, "some-id")));

        assertThat(out).isEmpty();
    }

    @Test
    void it_writes_no_message_to_summary() throws IOException {
        String out = renderAsSummary();
        assertThat(out).isEmpty();
    }

    @Test
    void it_throws_when_writing_after_close() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToUsageWriter writer = create(bytes);
        writer.close();
        assertThrows(IOException.class, () -> writer.write(null));
    }

    @Test
    void it_can_be_closed_twice() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToUsageWriter writer = create(bytes);
        writer.close();
        assertDoesNotThrow(writer::close);
    }

    private static String renderAsSummary(Envelope... messages) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MessagesToUsageWriter writer = create(bytes)) {
            for (Envelope message : messages) {
                writer.write(message);
            }
        }
        return new String(bytes.toByteArray(), UTF_8);
    }

    private static MessagesToUsageWriter create(ByteArrayOutputStream bytes) {
        return MessagesToUsageWriter.builder(UsageReportPlainTextSerializer.builder().build()).build(bytes);
    }
}
