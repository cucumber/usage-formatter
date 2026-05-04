import type { Query } from '@cucumber/query'

import type { UsageReport } from './UsageReport'

export function buildUsageReport(_query: Query): UsageReport {
  return { stepDefinitions: [] }
}
