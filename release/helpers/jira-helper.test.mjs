import assert from 'node:assert/strict';
import { afterEach, describe, it } from 'node:test';
import { getJiraIssuesOfVersions, getJiraVersions } from './jira-helper.mjs';

const realFetch = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = realFetch;
});

/** Stubs fetch with a handler that matches on url, so each test states only what it cares about. */
function stubFetch(handler) {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    const body = options.body ? JSON.parse(options.body) : undefined;
    calls.push({ url, body });
    const payload = await handler(url, body, calls.length);
    return { json: async () => payload };
  };
  return calls;
}

describe('getJiraVersions', () => {
  it('returns the version of every board that carries the name, and skips the ones that do not', async () => {
    stubFetch((url) => {
      if (url.includes('/project/APIM/'))
        return [
          { id: '100', name: '4.9.0' },
          { id: '101', name: '4.8.0' },
        ];
      if (url.includes('/project/AIAM/')) return [{ id: '200', name: '4.9.0' }];
      return []; // the other boards do not release this version
    });

    const versions = await getJiraVersions('4.9.0');

    assert.deepEqual(versions, [
      { projectKey: 'APIM', name: '4.9.0' },
      { projectKey: 'AIAM', name: '4.9.0' },
    ]);
  });

  it('keeps going when a board answers with an error instead of a list', async () => {
    // A board the token cannot read must not cost the release its whole changelog. Asserted on the
    // message too: without the shape check this still "works" by throwing into the catch, which reports
    // a TypeError rather than saying plainly that the board was skipped.
    stubFetch((url) => (url.includes('/project/APIM/') ? [{ id: '100', name: '4.9.0' }] : { errorMessages: ['No permission'] }));
    const warnings = [];
    const realWarn = console.warn;
    console.warn = (message) => warnings.push(message);

    try {
      const versions = await getJiraVersions('4.9.0');
      assert.deepEqual(
        versions.map((version) => version.projectKey),
        ['APIM'],
      );
    } finally {
      console.warn = realWarn;
    }

    assert.ok(
      warnings.some((warning) => warning.includes('OBS') && warning.includes('skipping it')),
      `expected a plain "skipping it" warning, got: ${JSON.stringify(warnings)}`,
    );
  });

  it('keeps going when a board request throws outright', async () => {
    stubFetch((url) => {
      if (url.includes('/project/OBS/')) throw new Error('network down');
      return url.includes('/project/APIM/') ? [{ id: '100', name: '4.9.0' }] : [];
    });

    assert.deepEqual(
      (await getJiraVersions('4.9.0')).map((version) => version.projectKey),
      ['APIM'],
    );
  });

  it('finds nothing when no board carries the version', async () => {
    stubFetch(() => [{ id: '999', name: '3.0.0' }]);

    assert.deepEqual(await getJiraVersions('4.9.0'), []);
  });
});

describe('getJiraIssuesOfVersions', () => {
  const publicBug = (key, project) => ({
    key,
    fields: {
      issuetype: { name: 'Public Bug' },
      summary: `${key} summary`,
      components: [{ name: 'Gateway' }],
      project: { key: project },
      customfield_10115: '42',
    },
  });

  it('asks for the version name, scoped to the boards that carry it', async () => {
    const calls = stubFetch(() => ({ issues: [] }));

    await getJiraIssuesOfVersions([
      { projectKey: 'APIM', name: '4.9.0' },
      { projectKey: 'AIAM', name: '4.9.0' },
    ]);

    assert.equal(calls[0].body.jql, 'project IN (APIM, AIAM) AND fixVersion = "4.9.0"');
  });

  it('reads every page rather than stopping at the first', async () => {
    // The search endpoint pages; stopping at page one silently truncated the changelog.
    const calls = stubFetch((_url, _body, call) =>
      call === 1 ? { issues: [publicBug('APIM-1', 'APIM')], nextPageToken: 'page-2' } : { issues: [publicBug('AIAM-2', 'AIAM')] },
    );

    const issues = await getJiraIssuesOfVersions([{ projectKey: 'APIM', name: '4.9.0' }]);

    assert.deepEqual(
      issues.map((issue) => issue.key),
      ['APIM-1', 'AIAM-2'],
    );
    assert.equal(calls[1].body.nextPageToken, 'page-2');
  });

  it('tags each ticket with the board it came from, so the changelog can group by product', async () => {
    stubFetch(() => ({ issues: [publicBug('APIM-1', 'APIM'), publicBug('OBS-9', 'OBS')] }));

    const issues = await getJiraIssuesOfVersions([{ projectKey: 'APIM', name: '4.9.0' }]);

    assert.deepEqual(
      issues.map((issue) => issue.project),
      ['APIM', 'OBS'],
    );
  });

  it('keeps only the public issue types', async () => {
    stubFetch(() => ({
      issues: [
        publicBug('APIM-1', 'APIM'),
        {
          key: 'APIM-2',
          fields: { issuetype: { name: 'Bug' }, summary: 'internal', components: [], project: { key: 'APIM' }, customfield_10115: '1' },
        },
      ],
    }));

    const issues = await getJiraIssuesOfVersions([{ projectKey: 'APIM', name: '4.9.0' }]);

    assert.deepEqual(
      issues.map((issue) => issue.key),
      ['APIM-1'],
    );
  });

  it('falls back to the remote link when the GitHub issue field is empty', async () => {
    stubFetch((url) => {
      if (url.includes('/remotelink')) {
        return [{ object: { url: 'https://github.com/gravitee-io/issues/issues/77' } }];
      }
      return {
        issues: [
          {
            key: 'AIAM-3',
            fields: { issuetype: { name: 'Public Bug' }, summary: 's', components: [], project: { key: 'AIAM' }, customfield_10115: null },
          },
        ],
      };
    });

    const issues = await getJiraIssuesOfVersions([{ projectKey: 'AIAM', name: '4.9.0' }]);

    assert.equal(issues[0].githubIssue, '77');
  });

  it('does not call the search endpoint at all when no board carries the version', async () => {
    const calls = stubFetch(() => ({ issues: [] }));

    assert.deepEqual(await getJiraIssuesOfVersions([]), []);
    assert.equal(calls.length, 0);
  });
});
