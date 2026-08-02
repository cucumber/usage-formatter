package io.cucumber.usageformatter;

import io.cucumber.messages.Convertor;
import io.cucumber.usageformatter.UsageReport.Statistics;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.math.MathContext.DECIMAL64;

final class Durations {

    private Durations() {
        /* no-op */
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
        var n = BigDecimal.valueOf(durations.size());
        var meanSeconds = toBigDecimalSeconds(mean);
        var varianceTimeN = durations.stream()
                .map(Durations::toBigDecimalSeconds)
                .map(durationSeconds -> durationSeconds.subtract(meanSeconds).pow(2))
                .reduce(BigDecimal::add)
                //.map(sum -> sum.divide(n, DECIMAL64))
                .orElse(ZERO);
        var stdError = varianceTimeN
                //.divide(n, DECIMAL64)
                .sqrt(DECIMAL64)
                // Rearranged to merge the other two divide by n operations.
                .divide(n, DECIMAL64);
        var z095 = BigDecimal.valueOf(2);
        var marginOfError = z095.multiply(stdError).divideAndRemainder(BigDecimal.ONE);

        long seconds = marginOfError[0].longValueExact();
        long nanos = marginOfError[1].scaleByPowerOfTen(9).longValue();
        return Duration.ofSeconds(seconds, nanos);
    }

    static BigDecimal toBigDecimalSeconds(Duration duration) {
        return BigDecimal.valueOf(duration.getSeconds()).add(BigDecimal.valueOf(duration.getNano(), 9));
    }

    static BigDecimal toBigDecimalSeconds(io.cucumber.messages.types.Duration duration) {
        return BigDecimal.valueOf(duration.getSeconds()).add(BigDecimal.valueOf(duration.getNanos(), 9));
    }
}
