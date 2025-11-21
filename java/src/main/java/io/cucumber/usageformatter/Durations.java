package io.cucumber.usageformatter;

import io.cucumber.messages.Convertor;
import io.cucumber.usageformatter.UsageReport.Statistics;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class Durations {

    private Durations() {
        // utility class
    }

    @Nullable
    static Statistics createStatistics(List<Duration> durations) {
        if (durations.isEmpty()) {
            return null;
        }

        Duration sum = durations.stream()
                .reduce(Duration::plus)
                // Can't happen
                .orElse(Duration.ZERO);
        Duration mean = sum.dividedBy(durations.size());
        Duration moe95 = calculateMarginOfError95(durations, mean);
        return new Statistics(
                Convertor.toMessage(sum),
                Convertor.toMessage(mean),
                Convertor.toMessage(moe95)
        );
    }

    /**
     * Calculate the margin of error with a 0.95% confidence interval.
     * <p>
     * So assuming a normal distribution, the duration of a step will fall
     * within {@code mean ± moe95} with 95% probability.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Margin_of_error">Wikipedia - Margin of error</a>
     */
    private static Duration calculateMarginOfError95(List<Duration> durations, Duration mean) {
        BigDecimal meanSeconds = toBigDecimalSeconds(mean);
        BigDecimal variance = durations.stream()
                .map(Durations::toBigDecimalSeconds)
                .map(durationSeconds -> durationSeconds.subtract(meanSeconds).pow(2))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        // TODO: With Java 17, use BigDecimal.sqrt and
        double marginOfError = 2 * Math.sqrt(variance.doubleValue()) / durations.size();
        // TODO: With Java 17, BigDecimal.divideAndRemainder for seconds and nanos
        long seconds = (long) Math.floor(marginOfError);
        long nanos = (long) Math.floor((marginOfError - seconds) * TimeUnit.SECONDS.toNanos(1));
        return Duration.ofSeconds(seconds, nanos);
    }

    static BigDecimal toBigDecimalSeconds(Duration duration) {
        return BigDecimal.valueOf(duration.getSeconds()).add(BigDecimal.valueOf(duration.getNano(), 9));
    }

    static BigDecimal toBigDecimalSeconds(io.cucumber.messages.types.Duration duration) {
        return BigDecimal.valueOf(duration.getSeconds()).add(BigDecimal.valueOf(duration.getNanos(), 9));
    }
}
