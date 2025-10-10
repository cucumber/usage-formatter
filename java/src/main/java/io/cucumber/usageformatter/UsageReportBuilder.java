package io.cucumber.usageformatter;

import io.cucumber.messages.Convertor;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.StepDefinition;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.query.Query;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

final class UsageReportBuilder {

    private final Query query;
    private final Function<String, String> uriFormatter;
    private final SourceReferenceFormatter sourceReferenceFormatter;

    UsageReportBuilder(Query query, Function<String, String> uriFormatter) {
        this.query = requireNonNull(query);
        this.uriFormatter = requireNonNull(uriFormatter);
        this.sourceReferenceFormatter = new SourceReferenceFormatter(uriFormatter);
    }

    UsageReport build() {
        Map<Optional<StepDefinition>, List<UsageReport.StepUsage>> testStepsFinishedByStepDefinition = query
                .findAllTestStepFinished()
                .stream()
                .collect(groupingBy(findUnambiguousStepDefinitionBy(), LinkedHashMap::new,
                        mapping(createStepUsage(), toList())));

        // Add unused step definitions
        query.findAllStepDefinitions().stream()
                .map(Optional::of)
                .forEach(stepDefinition -> testStepsFinishedByStepDefinition
                        .computeIfAbsent(stepDefinition, sd -> new ArrayList<>()));

        List<UsageReport.StepDefinitionUsage> stepDefinitionUsages = testStepsFinishedByStepDefinition.entrySet()
                .stream()
                // Filter out steps with without a step definition or with an
                // ambiguous step definition. These can't be represented.
                .filter(entry -> entry.getKey().isPresent())
                .map(entry -> createStepDefinitionUsage(entry.getKey().get(), entry.getValue()))
                .collect(toList());
        return new UsageReport(stepDefinitionUsages);
    }

    private UsageReport.StepDefinitionUsage createStepDefinitionUsage(StepDefinition stepDefinition, List<UsageReport.StepUsage> stepUsages) {
        return new UsageReport.StepDefinitionUsage(
                formatSource(stepDefinition),
                formatSourceReference(stepDefinition),
                createStatistics(stepUsages),
                stepUsages
        );
    }

    private String formatSource(StepDefinition stepDefinition) {
        return stepDefinition.getPattern().getSource();
    }

    private String formatSourceReference(StepDefinition stepDefinition) {
        return sourceReferenceFormatter.format(stepDefinition.getSourceReference()).orElse("");
    }

    private UsageReport.Statistics createStatistics(List<UsageReport.StepUsage> stepUsages) {
        List<Duration> durations = stepUsages.stream()
                .map(UsageReport.StepUsage::getDuration)
                .collect(toList());
        return Durations.createStatistics(durations);
    }

    private Function<TestStepFinished, UsageReport.StepUsage> createStepUsage() {
        return testStepFinished -> query
                .findTestStepBy(testStepFinished)
                .flatMap(query::findPickleStepBy)
                .map(pickleStep -> createStepUsage(testStepFinished, pickleStep))
                .orElseGet(() -> new UsageReport.StepUsage("", Duration.ZERO, ""));
    }

    private UsageReport.StepUsage createStepUsage(TestStepFinished testStepFinished, PickleStep pickleStep) {
        String text = pickleStep.getText();
        String location = findLocationOf(testStepFinished);
        Duration duration = Convertor.toDuration(testStepFinished.getTestStepResult().getDuration());
        return new UsageReport.StepUsage(text, duration, location);
    }

    private String findLocationOf(TestStepFinished testStepFinished) {
        return query.findPickleBy(testStepFinished)
                .map(pickle -> uriFormatter.apply(pickle.getUri()) + query.findLocationOf(pickle)
                        .map(Location::getLine)
                        .map(line -> ":" + line)
                        .orElse(""))
                .orElse("");
    }

    private Function<TestStepFinished, Optional<StepDefinition>> findUnambiguousStepDefinitionBy() {
        return testStepFinished -> query.findTestStepBy(testStepFinished)
                .flatMap(query::findUnambiguousStepDefinitionBy);
    }

}
