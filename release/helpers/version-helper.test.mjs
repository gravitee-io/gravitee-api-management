import assert from 'node:assert/strict';
import { afterEach, describe, it } from 'node:test';
import {
  DISTRIBUTION_POM,
  ROOT_POM,
  assertChartMatchesVersion,
  assertCoreTagIsFree,
  assertTagIsFree,
  assertVersionMatchesPoms,
  versionFromPom,
} from './version-helper.mjs';

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

describe('assertVersionMatchesPoms', () => {
  const realFetch = globalThis.fetch;
  const realChalk = globalThis.chalk;
  const realExit = process.exit;

  const pom = (version) => `<project><properties>
  <revision>${version}</revision><sha1 /><changelist>-SNAPSHOT</changelist>
</properties></project>`;

  /** Serves a pom per path; anything absent from the map answers 404, as GitHub would. */
  function stub(poms, { branchExists = true } = {}) {
    const asked = [];
    globalThis.fetch = async (url) => {
      if (url.includes('/branches/')) {
        return branchExists ? { ok: true, status: 200 } : { ok: false, status: 404, statusText: 'Not Found' };
      }
      const path = decodeURIComponent(url.split('/contents/')[1].split('?')[0]);
      asked.push(path);
      const body = poms[path];
      return body === undefined ? { ok: false, status: 404, statusText: 'Not Found' } : { ok: true, status: 200, text: async () => body };
    };
    globalThis.chalk = { red: (s) => s, yellow: (s) => s };
    process.exit = (code) => {
      throw new Error(`exit ${code}`);
    };
    return asked;
  }

  afterEach(() => {
    globalThis.fetch = realFetch;
    globalThis.chalk = realChalk;
    process.exit = realExit;
  });

  it('passes when every pom asked for carries the version', async () => {
    const asked = stub({ 'pom.xml': pom('4.12.16'), 'gravitee-apim-distribution/pom.xml': pom('4.12.16') });

    await assertVersionMatchesPoms('4.12.16', '4.12.x', [ROOT_POM, DISTRIBUTION_POM]);

    assert.deepEqual(asked, ['pom.xml', 'gravitee-apim-distribution/pom.xml']);
  });

  // A branch that does not exist answers 404 on every path, exactly as a missing file does. Read as
  // "this pom inherits the root version", that silence used to let a mistyped or not-yet-cut branch
  // through — and print something untrue on its way.
  // Every pom skipped means nothing was compared, and the command would have announced a check it
  // never made. That is the shape the distribution lane takes on a branch where it inherits.
  it('refuses when every pom asked for was skipped', async () => {
    stub({ 'gravitee-apim-distribution/pom.xml': '<project/>' });

    await assert.rejects(() => assertVersionMatchesPoms('4.12.20', '4.12.x', [DISTRIBUTION_POM]), { message: 'exit 1' });
  });

  it('refuses a branch the repository does not have', async () => {
    stub({}, { branchExists: false });

    await assert.rejects(() => assertVersionMatchesPoms('4.13.0', '4.13.x', [ROOT_POM]), { message: 'exit 1' });
  });

  it('refuses a pom the branch does not carry, rather than calling it inherited', async () => {
    stub({ 'pom.xml': pom('4.12.16') });

    await assert.rejects(() => assertVersionMatchesPoms('4.12.16', '4.12.x', [ROOT_POM, DISTRIBUTION_POM]), { message: 'exit 1' });
  });

  it('refuses when the distribution lags behind the root, which a core release makes it do', async () => {
    stub({ 'pom.xml': pom('4.12.16'), 'gravitee-apim-distribution/pom.xml': pom('4.12.15') });

    await assert.rejects(() => assertVersionMatchesPoms('4.12.16', '4.12.x', [ROOT_POM, DISTRIBUTION_POM]), { message: 'exit 1' });
  });

  it('refuses the root version too, which is what the old check already caught', async () => {
    stub({ 'pom.xml': pom('4.12.16'), 'gravitee-apim-distribution/pom.xml': pom('4.12.16') });

    await assert.rejects(() => assertVersionMatchesPoms('4.12.15', '4.12.x', [ROOT_POM, DISTRIBUTION_POM]), { message: 'exit 1' });
  });

  it('ignores the distribution on a branch where it inherits the root version', async () => {
    // Only master carries a triplet there; a support branch has the file without a <revision>.
    stub({ 'pom.xml': pom('4.11.9'), 'gravitee-apim-distribution/pom.xml': '<project/>' });

    await assertVersionMatchesPoms('4.11.9', '4.11.x', [ROOT_POM, DISTRIBUTION_POM]);
  });

  it('reads the root alone for a core release, the distribution staying behind on purpose', async () => {
    const asked = stub({ 'pom.xml': pom('4.13.0'), 'gravitee-apim-distribution/pom.xml': pom('4.12.14') });

    await assertVersionMatchesPoms('4.13.0', '4.13.x', [ROOT_POM]);

    assert.deepEqual(asked, ['pom.xml']);
  });
});

describe('assertChartMatchesVersion', () => {
  const realFetch = globalThis.fetch;
  const realChalk = globalThis.chalk;
  const realExit = process.exit;

  // `version:` appears again under dependencies, so only the line-anchored first one counts — the
  // same one the release's sed rewrites with `0,/^version:/`.
  const chart = (version, appVersion = version) => `apiVersion: v2
version: ${version}
appVersion: ${appVersion}
dependencies:
  - name: elasticsearch
    version: 19.10.9
`;

  function stub(files, { branchExists = true } = {}) {
    const asked = [];
    globalThis.fetch = async (url) => {
      if (url.includes('/branches/')) {
        return branchExists ? { ok: true, status: 200 } : { ok: false, status: 404, statusText: 'Not Found' };
      }
      const path = decodeURIComponent(url.split('/contents/')[1].split('?')[0]);
      asked.push(path);
      const body = files[path];
      return body === undefined ? { ok: false, status: 404, statusText: 'Not Found' } : { ok: true, status: 200, text: async () => body };
    };
    globalThis.chalk = { red: (s) => s, yellow: (s) => s };
    process.exit = (code) => {
      throw new Error(`exit ${code}`);
    };
    return asked;
  }

  afterEach(() => {
    globalThis.fetch = realFetch;
    globalThis.chalk = realChalk;
    process.exit = realExit;
  });

  it('passes when the chart already carries the version being released', async () => {
    const asked = stub({ 'helm/Chart.yaml': chart('4.13.0') });

    await assertChartMatchesVersion('4.13.0', 'master');

    assert.deepEqual(asked, ['helm/Chart.yaml']);
  });

  it('passes on a pre-release, qualifier included', async () => {
    stub({ 'helm/Chart.yaml': chart('4.13.0-alpha.1') });

    await assertChartMatchesVersion('4.13.0-alpha.1', 'master');
  });

  // The case the "have you removed the alpha versions?" prompt was reminding people about.
  it('refuses a leftover qualifier', async () => {
    stub({ 'helm/Chart.yaml': chart('4.13.0-alpha.5') });

    await assert.rejects(() => assertChartMatchesVersion('4.13.0', 'master'), { message: 'exit 1' });
  });

  it('refuses when appVersion drifted from version', async () => {
    stub({ 'helm/Chart.yaml': chart('4.13.0', '4.12.19') });

    await assert.rejects(() => assertChartMatchesVersion('4.13.0', 'master'), { message: 'exit 1' });
  });

  it("reads the line-anchored version, not a dependency's", async () => {
    stub({ 'helm/Chart.yaml': chart('4.13.0') });

    await assertChartMatchesVersion('4.13.0', 'master');
  });

  it('refuses a chart that carries no version at all', async () => {
    stub({ 'helm/Chart.yaml': 'apiVersion: v2\nname: apim\n' });

    await assert.rejects(() => assertChartMatchesVersion('4.13.0', 'master'), { message: 'exit 1' });
  });
});

describe('assertTagIsFree', () => {
  const realFetch = globalThis.fetch;
  const realChalk = globalThis.chalk;
  const realExit = process.exit;

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

  // The distribution takes the bare version; only the core lane prefixes its tags.
  it('passes on a bare tag nobody has pushed', async () => {
    const calls = stub(404);

    await assertTagIsFree('4.13.0');

    assert.ok(calls[0].endsWith('/git/ref/tags/4.13.0'), calls[0]);
  });

  it('refuses a bare tag that already exists', async () => {
    stub(200);

    await assert.rejects(() => assertTagIsFree('4.13.0'), { message: 'exit 1' });
  });

  it('refuses rather than guess when GitHub does not answer', async () => {
    stub(500);

    await assert.rejects(() => assertTagIsFree('4.13.0'), { message: 'exit 1' });
  });
});
