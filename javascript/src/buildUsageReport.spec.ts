import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import type { Envelope } from '@cucumber/messages'
import { Query } from '@cucumber/query'
import { expect } from 'chai'

import { buildUsageReport } from './buildUsageReport'
import type { UsageReport } from './UsageReport'

const sources = ['ambiguous', 'minimal', 'unused-steps', 'multiple-features']

const testdataDir = path.join(__dirname, '../../testdata/src')

async function buildFromNdjson(source: string): Promise<UsageReport> {
  const query = new Query()

  await pipeline(
    fs.createReadStream(path.join(testdataDir, `${source}.ndjson`), { encoding: 'utf-8' }),
    new NdjsonToMessageStream(),
    new Writable({
      objectMode: true,
      write(envelope: Envelope, _: BufferEncoding, callback) {
        query.update(envelope)
        callback()
      },
    })
  )

  return buildUsageReport(query)
}

describe('buildUsageReport', function () {
  this.timeout(10_000)

  for (const source of sources) {
    it(`${source}`, async () => {
      const actual = await buildFromNdjson(source)
      const expected = JSON.parse(
        fs.readFileSync(path.join(testdataDir, `${source}.json`), { encoding: 'utf-8' })
      )
      expect(actual).to.deep.eq(expected)
    })
  }
})
