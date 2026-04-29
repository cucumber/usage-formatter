import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import type { Envelope } from '@cucumber/messages'
import { expect } from 'chai'

import { UsageBuilder } from './UsageBuilder'

const sources = ['ambiguous', 'minimal', 'unused-steps', 'multiple-features']

const testdataDir = path.join(__dirname, '../../testdata/src')

async function buildUsageReport(source: string): Promise<string> {
  let content = ''
  const builder = new UsageBuilder((chunk) => {
    content += chunk
  })

  await pipeline(
    fs.createReadStream(path.join(testdataDir, `${source}.ndjson`), { encoding: 'utf-8' }),
    new NdjsonToMessageStream(),
    new Writable({
      objectMode: true,
      write(envelope: Envelope, _: BufferEncoding, callback) {
        builder.update(envelope)
        callback()
      },
    })
  )

  return content
}

describe('UsageBuilder', function () {
  this.timeout(10_000)

  describe('json', () => {
    for (const source of sources) {
      it(`${source}`, async () => {
        const content = await buildUsageReport(source)
        const expected = fs.readFileSync(path.join(testdataDir, `${source}.json`), {
          encoding: 'utf-8',
        })
        expect(content).to.eq(expected)
      })
    }
  })
})
