package io.cucumber.usageformatter;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class Durations {
    static UsageReport.Statistics createStatistics(List<Duration> durations) {
        if (durations.isEmpty()) {
            return null;
        }
        
        Duration sum = durations.stream()
                .reduce(Duration::plus)
                // Can't happen
                .orElse(Duration.ZERO);
        Duration mean = sum.dividedBy(durations.size());
        Duration moe95 = calculateMarginOfError95(durations, mean);
        return new UsageReport.Statistics(sum, mean, moe95);
    }

    /**
     * Calculate the margin of error with a 0.95% confidence interval.
     */
    private static Duration calculateMarginOfError95(List<Duration> durations, Duration mean) {
        BigDecimal meanSeconds = toBigDecimalSeconds(mean);
        BigDecimal variance = durations.stream()
                .map(Durations::toBigDecimalSeconds)
                .map(durationSeconds -> durationSeconds.subtract(meanSeconds).pow(2))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        // TODO: With Java 17, use BigDecimal.sqrt and
        // BigDecimal.divideAndRemainder for seconds and nos
        double marginOfError = 2 * Math.sqrt(variance.doubleValue()) / durations.size();
        long seconds = (long) Math.floor(marginOfError);
        long nanos = (long) Math.floor((marginOfError - seconds) * TimeUnit.SECONDS.toNanos(1));
        return Duration.ofSeconds(seconds, nanos);
    }

    static BigDecimal toBigDecimalSeconds(Duration duration) {
        return BigDecimal.valueOf(duration.getSeconds()).add(BigDecimal.valueOf(duration.getNano(), 9));
    }
}
