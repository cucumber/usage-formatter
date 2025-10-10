package io.cucumber.usageformatter;

import io.cucumber.usageformatter.UsageReport.Statistics;
import io.cucumber.usageformatter.UsageReport.StepDefinitionUsage;
import io.cucumber.usageformatter.UsageReport.StepUsage;

import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import static io.cucumber.usageformatter.Durations.toBigDecimalSeconds;
import static io.cucumber.usageformatter.UsageReportSerializer.PlainTextFeature.INCLUDE_STEPS;
import static java.lang.System.lineSeparator;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;

public final class UsageReportSerializer implements MessagesToUsageWriter.Serializer {

    private static final int INCLUDE_ALL_STEPS = -1;
    public final String[] headers = new String[]{"Expression/Text", "Duration", "Mean", "±", "Error", "Location"};
    public final boolean[] leftAlignHeader = {true, false, false, true, false, true};
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
        return formatTable(createTable(stepDefinitions));
    }

    private List<String[]> createTable(List<StepDefinitionUsage> stepDefinitions) {
        List<String[]> table = new ArrayList<>();
        table.add(headers);

        stepDefinitions
                .stream()
                .sorted(byMeanDurationDescending())
                .map(this::createRows)
                .forEach(table::addAll);

        return table;
    }

    private List<String[]> createRows(StepDefinitionUsage stepDefinitionUsage) {
        List<String[]> rows = new ArrayList<>();
        Statistics duration = stepDefinitionUsage.getDuration();

        // Add step definition row
        rows.add(new String[]{
                stepDefinitionUsage.getExpression(),
                duration == null ? "" : formatDuration(duration.getSum()),
                duration == null ? "" : formatDuration(duration.getMean()),
                duration == null ? "" : "±",
                duration == null ? "" : formatDuration(duration.getMoe95()),
                stepDefinitionUsage.getLocation()
        });

        if (!features.contains(INCLUDE_STEPS)) {
            return rows;
        }

        // Add rows for steps, if any
        List<StepUsage> steps = stepDefinitionUsage.getSteps();
        if (steps.isEmpty()) {
            rows.add(new String[]{
                    "  UNUSED",
                    "",
                    "",
                    "",
                    "",
                    ""
            });
            return rows;
        }

        steps.sort(comparing(StepUsage::getDuration).reversed());
        boolean includeAllSteps = maxStepsPerStepDefinition == INCLUDE_ALL_STEPS;
        int includeToIndex = includeAllSteps ? steps.size() : Math.min(maxStepsPerStepDefinition, steps.size());
        for (StepUsage stepUsage : steps.subList(0, includeToIndex)) {
            rows.add(new String[]{
                    "  " + stepUsage.getText(),
                    formatDuration(stepUsage.getDuration()),
                    "",
                    "",
                    "",
                    stepUsage.getLocation()
            });
        }
        if (steps.size() > includeToIndex) {
            rows.add(new String[]{
                    "  " + (steps.size() - includeToIndex) + " more",
                    "",
                    "",
                    "",
                    "",
                    ""
            });
        }
        
        return rows;
    }

    private String formatTable(List<String[]> table) {
        StringJoiner joiner = new StringJoiner(lineSeparator(), lineSeparator(), lineSeparator());
        int[] longestCellLengthInColumn = findLongestCellLengthInColumn(table);
        for (String[] row : table) {
            StringJoiner rowJoiner = new StringJoiner(" ");
            for (int j = 0; j < row.length; j++) {
                String newElement = renderCellWithPadding(
                        longestCellLengthInColumn[j],
                        row[j],
                        leftAlignHeader[j]
                );
                rowJoiner.add(newElement);
            }
            joiner.add(rowJoiner.toString());
        }
        return joiner.toString();
    }

    private static Comparator<StepDefinitionUsage> byMeanDurationDescending() {
        return comparing(StepDefinitionUsage::getDuration, nullsFirst(comparing(Statistics::getMean))).reversed();
    }

    private String formatDuration(Duration duration) {
        return toBigDecimalSeconds(duration).setScale(3, HALF_EVEN).toPlainString() + "s";
    }

    private static int[] findLongestCellLengthInColumn(List<String[]> renderedCells) {
        // always square and non-sparse.
        int width = renderedCells.get(0).length;
        int[] longestCellInColumnLength = new int[width];
        for (String[] row : renderedCells) {
            for (int colIndex = 0; colIndex < width; colIndex++) {
                int current = longestCellInColumnLength[colIndex];
                int candidate = row[colIndex].length();
                longestCellInColumnLength[colIndex] = Math.max(current, candidate);
            }
        }
        return longestCellInColumnLength;
    }

    private static String renderCellWithPadding(int width, String cell, boolean leftAlign) {
        StringBuilder result = new StringBuilder();
        if (leftAlign) {
            result.append(cell);
            padSpace(result, width - cell.length());
        } else {
            padSpace(result, width - cell.length());
            result.append(cell);
        }
        return result.toString();
    }

    private static void padSpace(StringBuilder result, int padding) {
        for (int i = 0; i < padding; i++) {
            result.append(" ");
        }
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
