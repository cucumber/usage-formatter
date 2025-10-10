package io.cucumber.usageformatter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.StringJoiner;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toList;

public final class UnusedReportSerializer implements MessagesToUsageWriter.Serializer {

    @Override
    public void writeValue(Writer writer, UsageReport value) throws IOException {
        writer.append(format(value));
    }

    private String format(UsageReport usageReport) {
        List<UsageReport.StepDefinitionUsage> stepDefinitions = usageReport.getStepDefinitions();
        List<UsageReport.StepDefinitionUsage> unusedSteps = stepDefinitions.stream()
                .filter(stepDefinitionUsage -> stepDefinitionUsage.getSteps().isEmpty())
                .collect(toList());

        StringJoiner joiner = new StringJoiner(lineSeparator(), lineSeparator(), lineSeparator());
        joiner.add(unusedSteps.size() + " unused step definition:");
        unusedSteps.forEach(entry -> {
            String location = entry.getLocation();
            String pattern = entry.getExpression();
            joiner.add(location + " # " + pattern);
        });
        return joiner.toString();
    }
}
