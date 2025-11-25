package io.cucumber.usageformatter;

import io.cucumber.messages.types.Duration;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.SourceReference;
import io.cucumber.messages.types.StepDefinitionPattern;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class UsageReport {
    private final List<StepDefinitionUsage> stepDefinitions;

    UsageReport(List<StepDefinitionUsage> stepDefinitions) {
        this.stepDefinitions = requireNonNull(stepDefinitions);
    }

    public List<StepDefinitionUsage> getStepDefinitions() {
        return stepDefinitions;
    }

    /**
     * Container for usage-entries of steps
     */
    public static final class StepDefinitionUsage {

        private final StepDefinitionPattern pattern;
        private final SourceReference sourceReference;
        private final @Nullable Statistics duration;
        private final List<StepUsage> matches;

        StepDefinitionUsage(
                StepDefinitionPattern pattern, SourceReference sourceReference, @Nullable Statistics duration, List<StepUsage> matches
        ) {
            this.pattern = requireNonNull(pattern);
            this.sourceReference = requireNonNull(sourceReference);
            this.duration = duration;
            this.matches = requireNonNull(matches);
        }

        public StepDefinitionPattern getExpression() {
            return pattern;
        }

        public @Nullable Statistics getDuration() {
            return duration;
        }

        public List<StepUsage> getMatches() {
            return matches;
        }

        public SourceReference getSourceReference() {
            return sourceReference;
        }
    }

    public static final class Statistics {
        private final Duration sum;
        private final Duration mean;
        private final Duration moe95;

        Statistics(Duration sum, Duration mean, Duration moe95) {
            this.sum = sum;
            this.mean = mean;
            this.moe95 = moe95;
        }

        public Duration getSum() {
            return sum;
        }

        public Duration getMean() {
            return mean;
        }

        /**
         * Margin of error with a 95% confidence interval.
         * <p>
         * So assuming a normal distribution, the duration of a step will fall
         * within {@code mean ± moe95} with 95% probability.
         *
         * @see <a href="https://en.wikipedia.org/wiki/Margin_of_error">Wikipedia - Margin of error</a>
         */
        public Duration getMoe95() {
            return moe95;
        }
    }

    public static final class StepUsage {

        private final String text;
        private final Duration duration;
        private final String uri;
        private final @Nullable Location location;

        StepUsage(String text, Duration duration, String uri, @Nullable Location location) {
            this.text = requireNonNull(text);
            this.duration = requireNonNull(duration);
            this.uri = requireNonNull(uri);
            this.location = location;
        }

        public Duration getDuration() {
            return duration;
        }

        public String getUri() {
            return uri;
        }

        public Optional<Location> getLocation() {
            return Optional.ofNullable(location);
        }

        public String getText() {
            return text;
        }
    }
}
