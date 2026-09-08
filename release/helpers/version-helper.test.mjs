import assert from 'node:assert/strict';
import { afterEach, describe, it } from 'node:test';
import { assertCoreTagIsFree, versionFromPom } from './version-helper.mjs';

const pom = (properties) => `<project>
  <artifactId>gravitee-api-management</artifactId>
  <version>\${revision}\${sha1}\${changelist}</version>
  <properties>
${properties}
  </properties>
</project>`;

describe('versionFromPom', () => {
  it('reads a final release, whose qualifier element is self-closing', () => {
    const xml = pom('    <revision>4.12.18</revision>\n    <sha1 />\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(xml), '4.12.18');
  });

  it('reads a final release whose qualifier element is empty', () => {
    const xml = pom('    <revision>4.12.18</revision>\n    <sha1></sha1>\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(xml), '4.12.18');
  });

  it('reads a pre-release', () => {
    const xml = pom('    <revision>4.13.0</revision>\n    <sha1>-alpha.1</sha1>\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(xml), '4.13.0-alpha.1');
  });

  it('reads a hotfix', () => {
    const xml = pom('    <revision>4.12.17</revision>\n    <sha1>-hotfix.2</sha1>\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(xml), '4.12.17-hotfix.2');
  });

  it('ignores the changelist, which the release clears', () => {
    const released = pom('    <revision>4.12.18</revision>\n    <sha1 />\n    <changelist />');
    const snapshot = pom('    <revision>4.12.18</revision>\n    <sha1 />\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(released), versionFromPom(snapshot));
  });

  it('refuses a pom that carries no revision, rather than guessing', () => {
    assert.throws(() => versionFromPom('<project><version>4.12.18</version></project>'), /revision/);
  });
});

describe('assertCoreTagIsFree', () => {
  const realFetch = globalThis.fetch;
  const realChalk = globalThis.chalk;
  const realExit = process.exit;

  /** Stubs what zx provides as globals, so the helper can run under plain `node --test`. */
  function stub(status) {
    const calls = [];
    globalThis.fetch = async (url) => {
      calls.push(url);
      return { ok: status === 200, status, statusText: `status ${status}` };
    };
    globalThis.chalk = { red: (s) => s, yellow: (s) => s };
    process.exit = (code) => {
      throw new Error(`exit ${code}`);
    };
    return calls;
  }

  afterEach(() => {
    globalThis.fetch = realFetch;
    globalThis.chalk = realChalk;
    process.exit = realExit;
  });

  it('passes when the tag does not exist yet', async () => {
    const calls = stub(404);

    await assertCoreTagIsFree('4.13.0');

    assert.equal(calls.length, 1);
    assert.ok(calls[0].endsWith('/git/ref/tags/core_4.13.0'), calls[0]);
  });

  it('refuses a version whose tag already exists', async () => {
    stub(200);

    await assert.rejects(() => assertCoreTagIsFree('4.13.0'), { message: 'exit 1' });
  });

  it('refuses rather than guess when GitHub does not answer', async () => {
    stub(500);

    await assert.rejects(() => assertCoreTagIsFree('4.13.0'), { message: 'exit 1' });
  });
});
