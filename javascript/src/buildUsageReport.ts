import type {
  StepDefinition,
  TestCaseStarted,
  TestStep,
  TestStepFinished,
} from '@cucumber/messages'
import type { Query } from '@cucumber/query'

import type { Statistics, StepDefinitionUsage, StepUsage, UsageReport } from './UsageReport'

export function buildUsageReport(query: Query): UsageReport {
  const matchesByStepDefinitionId = new Map<string, StepUsage[]>()
  const stepDefinitionsById = new Map<string, StepDefinition>()

  for (const testStepFinished of query.findAllTestStepFinished()) {
    const testStep = query.findTestStepBy(testStepFinished)
    if (!testStep) {
      continue
    }
    const testCaseStarted = query.findTestCaseStartedBy(testStepFinished)
    if (!testCaseStarted) {
      continue
    }
    // skip non-final attempts so retried steps aren't double-counted
    if (query.findTestCaseFinishedBy(testCaseStarted)?.willBeRetried) {
      continue
    }
    const stepDefinition = query.findUnambiguousStepDefinitionBy(testStep)
    if (!stepDefinition) {
      continue
    }
    const stepUsage = createStepUsage(query, testStepFinished, testStep, testCaseStarted)
    if (!stepUsage) {
      continue
    }

    if (!stepDefinitionsById.has(stepDefinition.id)) {
      stepDefinitionsById.set(stepDefinition.id, stepDefinition)
      matchesByStepDefinitionId.set(stepDefinition.id, [])
    }
    matchesByStepDefinitionId.get(stepDefinition.id)?.push(stepUsage)
  }

  for (const stepDefinition of query.findAllStepDefinitions()) {
    if (!stepDefinitionsById.has(stepDefinition.id)) {
      stepDefinitionsById.set(stepDefinition.id, stepDefinition)
      matchesByStepDefinitionId.set(stepDefinition.id, [])
    }
  }

  const stepDefinitions: StepDefinitionUsage[] = []
  for (const [id, stepDefinition] of stepDefinitionsById) {
    const matches = matchesByStepDefinitionId.get(id) ?? []
    stepDefinitions.push({
      sourceReference: stepDefinition.sourceReference,
      ...(matches.length > 0 ? { duration: computeStatistics(matches) } : {}),
      matches,
      expression: stepDefinition.pattern,
    })
  }

  return { stepDefinitions }
}

function createStepUsage(
  query: Query,
  testStepFinished: TestStepFinished,
  testStep: TestStep,
  testCaseStarted: TestCaseStarted
): StepUsage | undefined {
  const pickleStep = query.findPickleStepBy(testStep)
  if (!pickleStep) {
    return undefined
  }
  const pickle = query.findPickleBy(testCaseStarted)
  if (!pickle) {
    return undefined
  }
  const location = query.findLocationOf(pickle)
  return {
    text: pickleStep.text,
    duration: testStepFinished.testStepResult.duration,
    uri: pickle.uri,
    ...(location ? { location } : {}),
  }
}

function computeStatistics(matches: ReadonlyArray<StepUsage>): Statistics {
  const seconds = matches.map((m) => m.duration.seconds + m.duration.nanos / 1e9)
  const sum = seconds.reduce((a, b) => a + b, 0)
  const mean = sum / matches.length
  const variance = seconds.reduce((acc, s) => acc + (s - mean) ** 2, 0)
  const moe95 = (2 * Math.sqrt(variance)) / matches.length
  return {
    sum: secondsToDuration(sum),
    mean: secondsToDuration(mean),
    moe95: secondsToDuration(moe95),
  }
}

function secondsToDuration(value: number) {
  const seconds = Math.floor(value)
  const nanos = Math.floor((value - seconds) * 1e9)
  return { seconds, nanos }
}
