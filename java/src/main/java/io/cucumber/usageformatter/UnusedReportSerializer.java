package io.cucumber.usageformatter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.StringJoiner;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toList;

public final class UnusedReportSerializer implements MessagesToUsageWriter.Serializer {
    private final String[] headers = {"Location", "Expression"};
    private final boolean[] leftAlignColumn = {true, true};

    @Override
    public void writeValue(Writer writer, UsageReport value) throws IOException {
        writer.append(format(value));
    }

    private String format(UsageReport usageReport) {
        List<UsageReport.StepDefinitionUsage> stepDefinitions = usageReport.getStepDefinitions();
        List<UsageReport.StepDefinitionUsage> unusedStepDefinitions = stepDefinitions.stream()
                .filter(stepDefinitionUsage -> stepDefinitionUsage.getSteps().isEmpty())
                .collect(toList());

        StringJoiner joiner = new StringJoiner(lineSeparator(), lineSeparator(), "");
        joiner.add(unusedStepDefinitions.size() + " unused step definition(s)");

        Table table = new Table(headers);
        unusedStepDefinitions.forEach(entry ->
                table.add(entry.getLocation(), "# " + entry.getExpression()));

        if (table.getRows().size() > 1) {
            joiner.add(TableFormatter.format(table, leftAlignColumn));
        }

        return joiner.toString();
    }
}
