package io.cucumber.usageformatter;

import io.cucumber.usageformatter.UsageReport.Statistics;
import io.cucumber.usageformatter.UsageReport.StepDefinitionUsage;
import io.cucumber.usageformatter.UsageReport.StepUsage;

import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static io.cucumber.usageformatter.Durations.toBigDecimalSeconds;
import static io.cucumber.usageformatter.UsageReportSerializer.PlainTextFeature.INCLUDE_STEPS;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;

public final class UsageReportSerializer implements MessagesToUsageWriter.Serializer {

    private static final int INCLUDE_ALL_STEPS = -1;
    public final String[] headers = new String[]{"Expression/Text", "Duration", "Mean", "±", "Error", "Location"};
    public final boolean[] leftAlignColumn = {true, false, false, true, false, true};
    public final int maxStepsPerStepDefinition;
    private final Set<PlainTextFeature> features;

    private UsageReportSerializer(int maxStepsPerStepDefinition, Set<PlainTextFeature> features) {
        this.maxStepsPerStepDefinition = maxStepsPerStepDefinition;
        this.features = features;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void writeValue(Writer writer, UsageReport value) throws IOException {
        writer.append(format(value));
    }

    private String format(UsageReport usageReport) {
        List<StepDefinitionUsage> stepDefinitions = usageReport.getStepDefinitions();
        if (stepDefinitions.isEmpty()) {
            return "";
        }
        Table table = createTable(stepDefinitions);
        return TableFormatter.format(table, leftAlignColumn);
    }

    private Table createTable(List<StepDefinitionUsage> stepDefinitions) {
        return stepDefinitions
                .stream()
                .sorted(byMeanDurationDescending())
                .map(this::createRows)
                .reduce(new Table(headers), Table::addTo);
    }

    private Table createRows(StepDefinitionUsage stepDefinitionUsage) {
        Table table = new Table();
        Statistics duration = stepDefinitionUsage.getDuration();

        // Add step definition row
        table.add(
                stepDefinitionUsage.getExpression(),
                duration == null ? "" : formatDuration(duration.getSum()),
                duration == null ? "" : formatDuration(duration.getMean()),
                duration == null ? "" : "±",
                duration == null ? "" : formatDuration(duration.getMoe95()),
                stepDefinitionUsage.getLocation()
        );

        if (!features.contains(INCLUDE_STEPS)) {
            return table;
        }

        // Add rows for steps, if any
        List<StepUsage> steps = stepDefinitionUsage.getSteps();
        if (steps.isEmpty()) {
            table.add(
                    "  UNUSED",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
            return table;
        }

        boolean includeAllSteps = maxStepsPerStepDefinition == INCLUDE_ALL_STEPS;
        int includeToIndex = includeAllSteps ? steps.size() : Math.min(maxStepsPerStepDefinition, steps.size());

        steps.stream()
                .sorted(comparing(StepUsage::getDuration).reversed())
                .limit(includeToIndex)
                .forEach(stepUsage ->
                        table.add(
                                "  " + stepUsage.getText(),
                                formatDuration(stepUsage.getDuration()),
                                "",
                                "",
                                "",
                                stepUsage.getLocation()
                        ));

        if (steps.size() > includeToIndex) {
            table.add(
                    "  " + (steps.size() - includeToIndex) + " more",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }

        return table;
    }

    private static Comparator<StepDefinitionUsage> byMeanDurationDescending() {
        return comparing(StepDefinitionUsage::getDuration, nullsFirst(comparing(Statistics::getMean))).reversed();
    }

    private String formatDuration(Duration duration) {
        return toBigDecimalSeconds(duration).setScale(3, HALF_EVEN).toPlainString() + "s";
    }


    public static final class Builder {
        private final Set<PlainTextFeature> features = EnumSet.noneOf(PlainTextFeature.class);
        private int maxStepsPerStepDefinition = INCLUDE_ALL_STEPS;

        /**
         * Toggles a given feature.
         */
        public Builder feature(PlainTextFeature feature, boolean enabled) {
            if (enabled) {
                features.add(feature);
            } else {
                features.remove(feature);
            }
            return this;
        }

        /**
         * Limit the number of steps shown per step definition.
         * <p>
         * A negative value means all steps are included.
         */
        public Builder maxStepsPerStepDefinition(int n) {
            this.maxStepsPerStepDefinition = n < 0 ? INCLUDE_ALL_STEPS : n;
            return this;
        }

        public UsageReportSerializer build() {
            return new UsageReportSerializer(maxStepsPerStepDefinition, features);
        }
    }

    public enum PlainTextFeature {

        /**
         * Include steps using a step definition.
         */
        INCLUDE_STEPS,
    }
}
