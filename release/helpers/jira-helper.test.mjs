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
  it('returns the key of every board that carries the name, and skips the ones that do not', async () => {
    stubFetch((url) => {
      if (url.includes('/project/APIM/'))
        return [
          { id: '100', name: '4.9.0' },
          { id: '101', name: '4.8.0' },
        ];
      if (url.includes('/project/AIAM/')) return [{ id: '200', name: '4.9.0' }];
      return []; // the other boards do not release this version
    });

    const versions = await getJiraVersions('4.9.0', ['APIM', 'AIAM', 'OBS']);

    assert.deepEqual(versions, ['APIM', 'AIAM']);
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
      const versions = await getJiraVersions('4.9.0', ['APIM', 'OBS']);
      assert.deepEqual(versions, ['APIM']);
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

    assert.deepEqual(await getJiraVersions('4.9.0', ['APIM', 'OBS']), ['APIM']);
  });

  it('finds nothing when no board carries the version', async () => {
    stubFetch(() => [{ id: '999', name: '3.0.0' }]);

    assert.deepEqual(await getJiraVersions('4.9.0', ['APIM']), []);
  });

  it('defaults to the JIRA_PROJECTS boards without depending on module load order', async () => {
    // Reading the env at call time (rather than once at import) means a caller that sets JIRA_PROJECTS
    // for the real release job cannot make this test start querying a different set of boards.
    stubFetch((url) => (url.includes('/project/APIM/') ? [{ id: '100', name: '4.9.0' }] : []));
    const previous = process.env.JIRA_PROJECTS;
    process.env.JIRA_PROJECTS = 'APIM';
    try {
      assert.deepEqual(await getJiraVersions('4.9.0'), ['APIM']);
    } finally {
      process.env.JIRA_PROJECTS = previous;
    }
  });
});

describe('getJiraIssuesOfVersions', () => {
  const publicBug = (key) => ({
    key,
    fields: {
      issuetype: { name: 'Public Bug' },
      summary: `${key} summary`,
      components: [{ name: 'Gateway' }],
      customfield_10115: '42',
    },
  });

  it('asks for the version name, scoped to the boards that carry it, with keys quoted', async () => {
    const calls = stubFetch(() => ({ issues: [] }));

    await getJiraIssuesOfVersions(['APIM', 'AIAM'], '4.9.0');

    assert.equal(calls[0].body.jql, 'project IN ("APIM", "AIAM") AND fixVersion = "4.9.0"');
  });

  it('escapes a quote embedded in a project key so it cannot break out of the JQL string', async () => {
    const calls = stubFetch(() => ({ issues: [] }));

    await getJiraIssuesOfVersions(['AP"IM'], '4.9.0');

    assert.equal(calls[0].body.jql, 'project IN ("AP\\"IM") AND fixVersion = "4.9.0"');
  });

  it('reads every page rather than stopping at the first', async () => {
    // The search endpoint pages; stopping at page one silently truncated the changelog.
    const calls = stubFetch((_url, _body, call) =>
      call === 1 ? { issues: [publicBug('APIM-1')], nextPageToken: 'page-2' } : { issues: [publicBug('AIAM-2')] },
    );

    const issues = await getJiraIssuesOfVersions(['APIM'], '4.9.0');

    assert.deepEqual(
      issues.map((issue) => issue.key),
      ['APIM-1', 'AIAM-2'],
    );
    assert.equal(calls[1].body.nextPageToken, 'page-2');
  });

  it('throws when the search endpoint answers with an error instead of a page', async () => {
    // Silently swallowing an error-shaped response here would write and commit an empty changelog as if
    // the release genuinely shipped nothing public.
    stubFetch(() => ({ errorMessages: ['The value APIM does not exist for the field project.'] }));

    await assert.rejects(() => getJiraIssuesOfVersions(['APIM'], '4.9.0'), /APIM does not exist/);
  });

  it('keeps only the public issue types', async () => {
    stubFetch(() => ({
      issues: [
        publicBug('APIM-1'),
        {
          key: 'APIM-2',
          fields: { issuetype: { name: 'Bug' }, summary: 'internal', components: [], customfield_10115: '1' },
        },
      ],
    }));

    const issues = await getJiraIssuesOfVersions(['APIM'], '4.9.0');

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
            fields: { issuetype: { name: 'Public Bug' }, summary: 's', components: [], customfield_10115: null },
          },
        ],
      };
    });

    const issues = await getJiraIssuesOfVersions(['AIAM'], '4.9.0');

    assert.equal(issues[0].githubIssue, '77');
  });

  it('fetches remote links for the whole batch concurrently, not one ticket at a time', async () => {
    // A sequential loop only ever has one remote-link request in flight; if this regresses to one-at-a-
    // time, only one resolver below will have been pushed by the time we check.
    const pendingResolvers = [];
    stubFetch((url) => {
      if (url.includes('/remotelink')) {
        return new Promise((resolve) => pendingResolvers.push(() => resolve([])));
      }
      return {
        issues: [
          { key: 'AIAM-1', fields: { issuetype: { name: 'Public Bug' }, summary: 's1', components: [], customfield_10115: null } },
          { key: 'OBS-1', fields: { issuetype: { name: 'Public Bug' }, summary: 's2', components: [], customfield_10115: null } },
        ],
      };
    });

    const result = getJiraIssuesOfVersions(['AIAM', 'OBS'], '4.9.0');

    // Let the search-page call resolve and both remote-link lookups get dispatched before either completes.
    await new Promise((resolve) => setTimeout(resolve, 0));

    assert.equal(pendingResolvers.length, 2, 'both remote-link lookups should already be in flight');

    pendingResolvers.forEach((resolve) => resolve());
    await result;
  });

  it('matches each ticket missing the field to its own remote link, not another ticket in the batch', async () => {
    // Every non-APIM board leaves the GitHub field empty, so this fallback runs for the whole batch, not
    // the odd straggler. Fetching those lookups concurrently must not mix up which link belongs to which
    // ticket.
    stubFetch((url) => {
      if (url.includes('/remotelink')) {
        const [, key] = url.match(/\/issue\/([^/]+)\/remotelink/);
        const issueNumber = { 'AIAM-3': '77', 'OBS-9': '88' }[key];
        return [{ object: { url: `https://github.com/gravitee-io/issues/issues/${issueNumber}` } }];
      }
      return {
        issues: [
          { key: 'AIAM-3', fields: { issuetype: { name: 'Public Bug' }, summary: 's1', components: [], customfield_10115: null } },
          { key: 'OBS-9', fields: { issuetype: { name: 'Public Bug' }, summary: 's2', components: [], customfield_10115: null } },
        ],
      };
    });

    const issues = await getJiraIssuesOfVersions(['AIAM', 'OBS'], '4.9.0');

    assert.deepEqual(
      issues.map((issue) => [issue.key, issue.githubIssue]),
      [
        ['AIAM-3', '77'],
        ['OBS-9', '88'],
      ],
    );
  });

  it('does not call the search endpoint at all when no board carries the version', async () => {
    const calls = stubFetch(() => ({ issues: [] }));

    assert.deepEqual(await getJiraIssuesOfVersions([], '4.9.0'), []);
    assert.equal(calls.length, 0);
  });
});
