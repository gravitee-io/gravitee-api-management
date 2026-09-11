import assert from 'node:assert/strict';
import { afterEach, describe, it } from 'node:test';
import { confirm } from './option-helper.mjs';

describe('confirm', () => {
  const realQuestion = globalThis.question;
  const realChalk = globalThis.chalk;
  const realLog = console.log;
  const realExit = process.exit;

  /** Stubs what zx provides as globals, so the helper can run under plain `node --test`. */
  function stub(answer) {
    const printed = [];
    globalThis.question = async () => answer;
    globalThis.chalk = { blue: (s) => s, yellow: (s) => s, red: (s) => s };
    console.log = (line) => printed.push(line);
    process.exit = (code) => {
      throw new Error(`exit ${code}`);
    };
    return printed;
  }

  afterEach(() => {
    globalThis.question = realQuestion;
    globalThis.chalk = realChalk;
    console.log = realLog;
    process.exit = realExit;
  });

  for (const answer of ['y', 'Y', 'yes', 'YES', ' y ', 'y\n']) {
    it(`continues on ${JSON.stringify(answer)}`, async () => {
      stub(answer);

      await confirm('Should we continue?');
    });
  }

  // The empty answer is the one a stray Enter produces, and the old prompts tested for 'n', which
  // let it through. Every one of these used to mean "go ahead".
  for (const answer of ['', ' ', '\n', 'n', 'N', 'no', 'nope', 'maybe', 'yeah']) {
    it(`stops on ${JSON.stringify(answer)}`, async () => {
      stub(answer);

      await assert.rejects(() => confirm('Should we continue?'), { message: 'exit 1' });
    });
  }

  it('prints the refusal it was given', async () => {
    const printed = stub('n');

    await assert.rejects(() => confirm('Should we continue?', 'Nothing was triggered.'), { message: 'exit 1' });

    assert.deepEqual(printed, ['Nothing was triggered.']);
  });

  it('says nothing when the answer is yes', async () => {
    const printed = stub('y');

    await confirm('Should we continue?');

    assert.deepEqual(printed, []);
  });
});
