package io.cucumber.usageformatter;

import io.cucumber.messages.Convertor;
import io.cucumber.messages.types.StepDefinition;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.query.Query;
import io.cucumber.usageformatter.UsageReport.StepUsage;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

final class UsageReportBuilder {

    private final Query query;

    UsageReportBuilder(Query query) {
        this.query = requireNonNull(query);
    }

    UsageReport build() {
        Map<Optional<StepDefinition>, List<Optional<StepUsage>>> testStepsFinishedByStepDefinition = query
                .findAllTestStepFinished()
                .stream()
                .collect(groupingBy(this::findUnambiguousStepDefinitionBy, LinkedHashMap::new, mapping(this::createStepUsage, toList())));

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
                .map(entry -> createStepDefinitionUsage(entry.getKey().get(), flatten(entry.getValue())))
                .collect(toList());
        return new UsageReport(stepDefinitionUsages);
    }

    private UsageReport.StepDefinitionUsage createStepDefinitionUsage(StepDefinition stepDefinition, List<StepUsage> matches) {
        return new UsageReport.StepDefinitionUsage(
                stepDefinition.getPattern(),
                stepDefinition.getSourceReference(),
                createStatistics(matches),
                matches
        );
    }

    private UsageReport.@Nullable Statistics createStatistics(List<StepUsage> stepUsages) {
        List<Duration> durations = stepUsages.stream()
                .map(StepUsage::getDuration)
                .map(Convertor::toDuration)
                .collect(toList());
        return Durations.createStatistics(durations);
    }

    private Optional<StepUsage> createStepUsage(TestStepFinished testStepFinished) {
        return query.findTestStepBy(testStepFinished)
                .flatMap(query::findPickleStepBy)
                .flatMap(pickleStep -> query
                        .findPickleBy(testStepFinished)
                        .map(pickle -> new StepUsage(
                                        pickleStep.getText(),
                                        testStepFinished.getTestStepResult().getDuration(),
                                        pickle.getUri(),
                                        query.findLocationOf(pickle).orElse(null)
                                )
                        ));
    }

    private Optional<StepDefinition> findUnambiguousStepDefinitionBy(TestStepFinished testStepFinished) {
        return query.findTestStepBy(testStepFinished)
                .flatMap(query::findUnambiguousStepDefinitionBy);
    }

    private static List<StepUsage> flatten(List<Optional<StepUsage>> value) {
        return value.stream().filter(Optional::isPresent).map(Optional::get).collect(toList());
    }
}
