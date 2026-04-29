import type { Envelope } from '@cucumber/messages'
import { Query } from '@cucumber/query'

export class UsageBuilder {
  private readonly query = new Query()

  constructor(private readonly write: (content: string) => void) {}

  update(envelope: Envelope): void {
    this.query.update(envelope)
    if (envelope.testRunFinished) {
      this.write(JSON.stringify({ stepDefinitions: [] }))
    }
  }
}
