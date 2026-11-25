import assert from 'node:assert/strict';
import { afterEach, describe, it } from 'node:test';
import { closeJiraRelease, getUnclosedJiraIssuesOfVersion, nextPatchVersion } from './jira-helper.mjs';

const realFetch = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = realFetch;
});

function stubFetch(handler) {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    const body = options.body ? JSON.parse(options.body) : undefined;
    calls.push({ url, method: options.method, body });
    const payload = await handler(url, options.method, body);
    return { json: async () => payload };
  };
  return calls;
}

/** A Jira where APIM carries the given versions, every other board none, and the search finds `unclosed`. */
function jira({ apim, unclosed = [], write = (body) => ({ id: '999', ...body }) }) {
  return stubFetch((url, method, body) => {
    if (url.includes('/project/APIM/versions')) return apim;
    if (url.includes('/project/')) return [];
    if (url.endsWith('/search/jql')) return { issues: unclosed };
    if (url.includes('/rest/api/3/version')) return write(body);
    throw new Error(`unexpected call ${method} ${url}`);
  });
}

const writes = (calls) =>
  calls.filter((call) => call.method === 'POST' || call.method === 'PUT').filter((call) => !call.url.endsWith('/search/jql'));
const quiet = () => {};

describe('nextPatchVersion', () => {
  it('bumps the patch of a plain release', () => {
    assert.equal(nextPatchVersion('4.13.6'), '4.13.7');
    assert.equal(nextPatchVersion('4.14.0'), '4.14.1');
    assert.equal(nextPatchVersion('4.10.99'), '4.10.100');
  });

  it('has no answer for an alpha or a hotfix', () => {
    assert.equal(nextPatchVersion('4.14.0-alpha.1'), undefined);
    assert.equal(nextPatchVersion('4.13.6-hotfix.1'), undefined);
  });
});

describe('closeJiraRelease', () => {
  it('releases the version and opens the next one on APIM', async () => {
    const calls = jira({ apim: [{ id: '100', name: '4.13.6', projectId: 10000, released: false }] });

    const result = await closeJiraRelease({ version: '4.13.6', today: '2026-11-22', log: quiet });

    assert.deepEqual(
      writes(calls).map(({ url, method, body }) => ({ url, method, body })),
      [
        {
          url: 'https://gravitee.atlassian.net/rest/api/3/version/100',
          method: 'PUT',
          body: { released: true, releaseDate: '2026-11-22' },
        },
        { url: 'https://gravitee.atlassian.net/rest/api/3/version', method: 'POST', body: { name: '4.13.7', projectId: 10000 } },
      ],
    );
    assert.equal(result.released, '4.13.6');
    assert.equal(result.created, '4.13.7');
  });

  it('writes nothing when the version is already released and the next one exists', async () => {
    const calls = jira({
      apim: [
        { id: '100', name: '4.13.6', projectId: 10000, released: true },
        { id: '101', name: '4.13.7', projectId: 10000, released: false },
      ],
    });

    const result = await closeJiraRelease({ version: '4.13.6', today: '2026-11-22', log: quiet });

    assert.deepEqual(writes(calls), []);
    assert.equal(result.released, undefined);
    assert.equal(result.created, undefined);
  });

  it('writes nothing in a dry run, but says what it would do', async () => {
    const calls = jira({ apim: [{ id: '100', name: '4.13.6', projectId: 10000, released: false }] });
    const messages = [];

    const result = await closeJiraRelease({ version: '4.13.6', today: '2026-11-22', dryRun: true, log: (m) => messages.push(m) });

    assert.deepEqual(writes(calls), []);
    assert.equal(result.created, '4.13.7');
    assert.ok(
      messages.some((m) => m.startsWith('[dry run] Creating APIM 4.13.7')),
      JSON.stringify(messages),
    );
  });

  it('still opens the next version when the released one was never created', async () => {
    // The very case this job exists for: someone forgot to create it, so no fix version could be set.
    const calls = jira({ apim: [{ id: '90', name: '4.13.5', projectId: 10000, released: true }] });
    const messages = [];

    const result = await closeJiraRelease({ version: '4.13.6', today: '2026-11-22', log: (m) => messages.push(m) });

    assert.deepEqual(
      writes(calls).map(({ method, body }) => ({ method, body })),
      [{ method: 'POST', body: { name: '4.13.7', projectId: 10000 } }],
    );
    assert.equal(result.released, undefined);
    assert.ok(
      messages.some((m) => m.includes('has no 4.13.6 version')),
      JSON.stringify(messages),
    );
  });

  it('does not touch Jira for a version that is not a plain release', async () => {
    const calls = stubFetch(() => {
      throw new Error('no call expected');
    });

    const result = await closeJiraRelease({ version: '4.14.0-alpha.1', today: '2026-11-22', log: quiet });

    assert.deepEqual(calls, []);
    assert.deepEqual(result, { skipped: true, released: undefined, created: undefined, unclosed: [] });
  });

  it('returns a refused write instead of throwing, with the report already read', async () => {
    jira({
      apim: [{ id: '100', name: '4.13.6', projectId: 10000, released: false }],
      unclosed: [
        { key: 'APIM-16008', fields: { issuetype: { name: 'Public Bug' }, status: { name: 'In Testing' }, summary: 'Archiving' } },
      ],
      write: () => ({ errorMessages: ['You do not have permission to edit versions.'] }),
    });

    const result = await closeJiraRelease({ version: '4.13.6', today: '2026-11-22', log: quiet });

    assert.match(result.error?.message, /permission to edit versions/);
    assert.deepEqual(
      result.unclosed.map((issue) => issue.key),
      ['APIM-16008'],
    );
    assert.equal(result.released, undefined);
  });

  it('returns an unreadable list of APIM versions as an error, and creates nothing', async () => {
    const calls = jira({ apim: { errorMessages: ['No permission'] } });

    const result = await closeJiraRelease({ version: '4.13.6', today: '2026-11-22', log: quiet });

    assert.match(result.error?.message, /versions of APIM: No permission/);
    assert.deepEqual(writes(calls), []);
  });

  it('reports the tickets that are not closed, and leaves them as they are', async () => {
    const calls = jira({
      apim: [{ id: '100', name: '4.13.6', projectId: 10000, released: false }],
      unclosed: [
        {
          key: 'APIM-16008',
          fields: { issuetype: { name: 'Public Bug' }, status: { name: 'In Testing' }, summary: 'Archiving an application' },
        },
      ],
    });

    const result = await closeJiraRelease({ version: '4.13.6', today: '2026-11-22', log: quiet });

    assert.deepEqual(result.unclosed, [
      { key: 'APIM-16008', type: 'Public Bug', status: 'In Testing', summary: 'Archiving an application' },
    ]);
    // Only the version itself is written: no transition, no fix version moved.
    assert.ok(writes(calls).every((call) => call.url.includes('/rest/api/3/version')));
  });
});

describe('getUnclosedJiraIssuesOfVersion', () => {
  it('searches the given boards for tickets of the version that are not done', async () => {
    const calls = stubFetch(() => ({ issues: [] }));

    await getUnclosedJiraIssuesOfVersion(['APIM', 'AIAM'], '4.13.6');

    assert.equal(calls[0].body.jql, 'project IN ("APIM", "AIAM") AND fixVersion = "4.13.6" AND statusCategory != Done ORDER BY key');
  });

  it('reads every page', async () => {
    const issue = (key) => ({ key, fields: { issuetype: { name: 'Story' }, status: { name: 'In Progress' }, summary: key } });
    stubFetch((url, method, body) =>
      body.nextPageToken ? { issues: [issue('APIM-2')] } : { issues: [issue('APIM-1')], nextPageToken: 't' },
    );

    const issues = await getUnclosedJiraIssuesOfVersion(['APIM'], '4.13.6');

    assert.deepEqual(
      issues.map((i) => i.key),
      ['APIM-1', 'APIM-2'],
    );
  });

  it('throws when the search answers with an error, rather than reporting a clean release', async () => {
    stubFetch(() => ({ errorMessages: ['bad JQL'] }));

    await assert.rejects(() => getUnclosedJiraIssuesOfVersion(['APIM'], '4.13.6'), /bad JQL/);
  });

  it('does not search when there is no board', async () => {
    const calls = stubFetch(() => ({ issues: [] }));

    assert.deepEqual(await getUnclosedJiraIssuesOfVersion([], '4.13.6'), []);
    assert.deepEqual(calls, []);
  });
});
