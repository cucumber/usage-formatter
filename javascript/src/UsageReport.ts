import type { Duration, Location, SourceReference, StepDefinitionPattern } from '@cucumber/messages'

export interface UsageReport {
  stepDefinitions: ReadonlyArray<StepDefinitionUsage>
}

export interface StepDefinitionUsage {
  sourceReference: SourceReference
  duration?: Statistics
  matches: ReadonlyArray<StepUsage>
  expression: StepDefinitionPattern
}

export interface Statistics {
  sum: Duration
  mean: Duration
  moe95: Duration
}

export interface StepUsage {
  text: string
  duration: Duration
  uri: string
  location?: Location
}
