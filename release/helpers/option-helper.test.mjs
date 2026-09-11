import assert from 'node:assert/strict';
import { afterEach, describe, it } from 'node:test';
import { confirm, isDryRun } from './option-helper.mjs';

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

describe('isDryRun', () => {
  const realArgv = globalThis.argv;
  const realChalk = globalThis.chalk;
  const realLog = console.log;
  const realExit = process.exit;

  function stub(args) {
    globalThis.argv = { _: [], ...args };
    globalThis.chalk = { red: (s) => s };
    console.log = () => {};
    process.exit = (code) => {
      throw new Error(`exit ${code}`);
    };
  }

  afterEach(() => {
    globalThis.argv = realArgv;
    globalThis.chalk = realChalk;
    console.log = realLog;
    process.exit = realExit;
  });

  it('is a rehearsal when the flag is there', () => {
    stub({ 'dry-run': true });

    assert.equal(isDryRun(), true);
  });

  it('is a real release when no flag is given', () => {
    stub({ version: '4.13.0' });

    assert.equal(isDryRun(), false);
  });

  // Errs on the safe side, and worth knowing: the value is not read, only the flag's presence.
  it('is still a rehearsal when the flag carries a value', () => {
    stub({ 'dry-run': 'false' });

    assert.equal(isDryRun(), true);
  });

  for (const flag of ['dryrun', 'dry_run', 'dryRun', 'DRY-RUN', 'Dry_Run', 'dry']) {
    it(`refuses --${flag} rather than reading it as a real release`, () => {
      stub({ [flag]: true, version: '4.13.0' });

      assert.throws(() => isDryRun(), { message: 'exit 1' });
    });
  }

  it('leaves unrelated options alone', () => {
    stub({ version: '4.13.0', branch: 'master', latest: true });

    assert.equal(isDryRun(), false);
  });
});
