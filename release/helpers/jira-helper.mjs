export const JiraBoards = ['APIM', 'FOUND', 'BX', 'ESM', 'AIAM', 'PORTAL', 'OBS', 'AUTHZ'];

function jiraProjects() {
  return (process.env.JIRA_PROJECTS ?? JiraBoards.join(','))
    .split(',')
    .map((key) => key.trim())
    .filter(Boolean);
}

function jiraHeaders() {
  return {
    Authorization: `Basic ${process.env.JIRA_TOKEN}`,
    Accept: 'application/json',
  };
}

/**
 * The keys of the projects that have a version named `versionName`.
 *
 * <p>A project that does not release this version simply has none, so an unreadable board is skipped
 * rather than failing the run: prereleases match no board at all, and one bad board must not cost a
 * release its changelog. Only when not one board could be read does empty stop meaning "nothing to
 * release" — the caller takes that as a clean no-op, so it throws rather than publish nothing.
 *
 * @param versionName {string} The version name
 * @param projectKeys {Array<string>} The projects to check; defaults to JIRA_PROJECTS
 * @returns {Promise<Array<string>>}
 */
export async function getJiraVersions(versionName, projectKeys = jiraProjects()) {
  const outcomes = await Promise.all(
    projectKeys.map(async (projectKey) => {
      try {
        const versions = await fetch(`https://gravitee.atlassian.net/rest/api/3/project/${projectKey}/versions`, {
          method: 'GET',
          headers: jiraHeaders(),
        }).then((response) => response.json());

        if (!Array.isArray(versions)) {
          // A project the token cannot read answers with an error object rather than a list.
          console.warn(`⚠️  Could not read versions of project ${projectKey}, skipping it.`);
          return { projectKey, readable: false };
        }

        return { projectKey, readable: true, carriesVersion: versions.some((candidate) => candidate.name === versionName) };
      } catch (error) {
        console.warn(`⚠️  Could not read versions of project ${projectKey}: ${error.message}`);
        return { projectKey, readable: false };
      }
    }),
  );

  const unreadable = outcomes.filter((outcome) => !outcome.readable).map((outcome) => outcome.projectKey);

  if (outcomes.length > 0 && unreadable.length === outcomes.length) {
    throw new Error(
      `Could not read the versions of any board (${unreadable.join(', ')}), so whether ${versionName} releases anything is unknown.`,
    );
  }

  return outcomes.filter((outcome) => outcome.carriesVersion).map((outcome) => outcome.projectKey);
}

/**
 * Every public ticket fixed in the given version, across the given projects.
 *
 * <p>Matched on the version name rather than a per-board version id: an issue can only ever carry its own
 * project's version, so the project clause already stops one board's version matching another's of the
 * same name.
 *
 * @param projectKeys {Array<string>}
 * @param versionName {string}
 * @returns {Promise<Array<{key: string, project: string, githubIssue: string, summary: string, components: Array<object>, type: string}>>}
 */
export async function getJiraIssuesOfVersions(projectKeys, versionName) {
  if (projectKeys.length === 0) {
    return [];
  }

  const projects = projectKeys.map((key) => `"${key.replace(/"/g, '\\"')}"`).join(', ');
  const jql = `project IN (${projects}) AND fixVersion = "${versionName}"`;

  const issuesFromJira = [];
  let nextPageToken;
  do {
    const page = await fetch('https://gravitee.atlassian.net/rest/api/3/search/jql', {
      method: 'POST',
      headers: { ...jiraHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({
        jql,
        maxResults: 100,
        fields: ['issuetype', 'summary', 'components', 'customfield_10115'],
        ...(nextPageToken ? { nextPageToken } : {}),
      }),
    }).then((response) => response.json());

    if (!Array.isArray(page.issues)) {
      // Read as "zero issues" this would commit an empty changelog as if nothing public had shipped.
      throw new Error(`Jira search failed: ${(page.errorMessages ?? ['unknown error']).join(', ')}`);
    }

    issuesFromJira.push(...page.issues);
    nextPageToken = page.nextPageToken;
  } while (nextPageToken);

  const issues = issuesFromJira
    .filter(
      (issue) =>
        issue.fields.issuetype.name === 'Public Bug' ||
        issue.fields.issuetype.name === 'Public Security' ||
        issue.fields.issuetype.name === 'Public Improvement',
    )
    .map((issue) => ({
      key: issue.key,
      // A Jira key is `<PROJECT>-<number>` and a project key cannot contain a hyphen, so the prefix is
      // the board — no need to ask for the field.
      project: issue.key.split('-')[0],
      githubIssue: issue.fields.customfield_10115,
      summary: issue.fields.summary,
      components: issue.fields.components ?? [],
      type: issue.fields.issuetype.name,
    }));

  // Boards other than APIM never fill in the GitHub issue field, so this runs for most of a non-APIM
  // batch rather than the odd straggler.
  await Promise.all(
    issues
      .filter((issue) => !issue.githubIssue)
      .map(async (issue) => {
        const remoteLinks = await fetch(`https://gravitee.atlassian.net/rest/api/3/issue/${issue.key}/remotelink`, {
          method: 'GET',
          headers: jiraHeaders(),
        }).then((response) => response.json());

        if (!Array.isArray(remoteLinks)) {
          // A ticket with no remote link answers with an empty list, so an error shape means the lookup
          // itself failed.
          console.warn(`⚠️  Could not read the remote links of ${issue.key}, its changelog line will have no GitHub link.`);
          return;
        }
        const githubIssue = remoteLinks.find((remoteLink) =>
          remoteLink.object.url.includes('https://github.com/gravitee-io/issues/issues/'),
        );
        if (githubIssue) {
          issue.githubIssue = githubIssue.object.url.replace('https://github.com/gravitee-io/issues/issues/', '');
        }
      }),
  );

  return issues;
}

/** The board the release versions live on. Another automation copies each new one to the other boards. */
export const JiraReleaseBoard = 'APIM';

const PLAIN_VERSION = /^(\d+)\.(\d+)\.(\d+)$/;

/**
 * The patch version that follows a release, or undefined for a version that has no successor on its line
 * (an alpha, a hotfix): those are released from a tag, and the line's next version is not theirs to open.
 *
 * @param version {string} for example '4.13.6'
 * @returns {string|undefined} for example '4.13.7'
 */
export function nextPatchVersion(version) {
  const match = PLAIN_VERSION.exec(version);
  return match ? `${match[1]}.${match[2]}.${Number(match[3]) + 1}` : undefined;
}

/**
 * Every version of a board, released or not.
 *
 * @param projectKey {string}
 * @returns {Promise<Array<{id: string, name: string, projectId: number, released: boolean}>>}
 */
export async function getJiraProjectVersions(projectKey) {
  const versions = await fetch(`https://gravitee.atlassian.net/rest/api/3/project/${projectKey}/versions`, {
    method: 'GET',
    headers: jiraHeaders(),
  }).then((response) => response.json());

  if (!Array.isArray(versions)) {
    throw new Error(`Could not read the versions of ${projectKey}: ${(versions.errorMessages ?? ['unknown error']).join(', ')}`);
  }
  return versions;
}

async function writeJiraVersion(url, method, body) {
  const version = await fetch(url, {
    method,
    headers: { ...jiraHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then((response) => response.json());

  if (!version?.id) {
    // Writing a version needs the "Administer projects" permission, which reading it does not.
    const reasons = [...(version?.errorMessages ?? []), ...Object.values(version?.errors ?? {})];
    throw new Error(
      `Jira refused to ${method === 'POST' ? 'create' : 'update'} ${body.name ?? 'the version'}: ${reasons.join(', ') || 'unknown error'}`,
    );
  }
  return version;
}

/**
 * Every ticket carrying the version that is not closed yet, whatever its type, across the given boards.
 *
 * @param projectKeys {Array<string>}
 * @param versionName {string}
 * @returns {Promise<Array<{key: string, type: string, status: string, summary: string}>>}
 */
export async function getUnclosedJiraIssuesOfVersion(projectKeys, versionName) {
  if (projectKeys.length === 0) {
    return [];
  }

  const projects = projectKeys.map((key) => `"${key.replace(/"/g, '\\"')}"`).join(', ');
  const jql = `project IN (${projects}) AND fixVersion = "${versionName}" AND statusCategory != Done ORDER BY key`;

  const issues = [];
  let nextPageToken;
  do {
    const page = await fetch('https://gravitee.atlassian.net/rest/api/3/search/jql', {
      method: 'POST',
      headers: { ...jiraHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({
        jql,
        maxResults: 100,
        fields: ['issuetype', 'summary', 'status'],
        ...(nextPageToken ? { nextPageToken } : {}),
      }),
    }).then((response) => response.json());

    if (!Array.isArray(page.issues)) {
      // Read as "zero issues" this would report a clean release.
      throw new Error(`Jira search failed: ${(page.errorMessages ?? ['unknown error']).join(', ')}`);
    }

    issues.push(...page.issues);
    nextPageToken = page.nextPageToken;
  } while (nextPageToken);

  return issues.map((issue) => ({
    key: issue.key,
    type: issue.fields.issuetype.name,
    status: issue.fields.status.name,
    summary: issue.fields.summary,
  }));
}

/**
 * What a release leaves to do in Jira: mark its version released, and open the next one so the automation
 * that sets fix versions on merge has somewhere to put the next fixes.
 *
 * <p>Tickets of the version that are not closed are reported, never moved nor transitioned: they did ship, and
 * whoever reads the report decides what that means.
 *
 * <p>The report is read first, and a failure to update the versions afterwards is returned rather than thrown, so
 * a refused write — writing versions needs "Administer projects", reading them does not — never costs the report.
 * The caller publishes it, then fails on `error`.
 *
 * @param options {{version: string, today: string, dryRun?: boolean, log?: (message: string) => void}}
 * @returns {Promise<{skipped: boolean, released: string|undefined, created: string|undefined, unclosed: Array<object>, error?: Error}>}
 */
export async function closeJiraRelease({ version, today, dryRun = false, log = console.log }) {
  const next = nextPatchVersion(version);
  if (!next) {
    log(`${version} is not a plain release, nothing to do in Jira.`);
    return { skipped: true, released: undefined, created: undefined, unclosed: [] };
  }

  const unclosed = await getUnclosedJiraIssuesOfVersion(await getJiraVersions(version), version);
  const prefix = dryRun ? '[dry run] ' : '';

  let released;
  let created;
  try {
    const versions = await getJiraProjectVersions(JiraReleaseBoard);
    const current = versions.find((candidate) => candidate.name === version);

    if (!current) {
      log(`⚠️  ${JiraReleaseBoard} has no ${version} version: no fix version was set on merge, so nothing to release.`);
    } else if (current.released) {
      log(`${JiraReleaseBoard} ${version} is already released.`);
    } else {
      log(`${prefix}Releasing ${JiraReleaseBoard} ${version} on ${today}.`);
      if (!dryRun) {
        // No moveUnfixedIssuesTo: tickets that are not closed keep this version.
        await writeJiraVersion(`https://gravitee.atlassian.net/rest/api/3/version/${current.id}`, 'PUT', {
          released: true,
          releaseDate: today,
        });
      }
      released = version;
    }

    if (versions.some((candidate) => candidate.name === next)) {
      log(`${JiraReleaseBoard} ${next} already exists.`);
    } else {
      // Any version of the board tells its id, the one just released included.
      const projectId = current?.projectId ?? versions[0]?.projectId;
      if (projectId === undefined) {
        throw new Error(`${JiraReleaseBoard} has no version at all, so its project id is unknown.`);
      }
      log(`${prefix}Creating ${JiraReleaseBoard} ${next}.`);
      if (!dryRun) {
        await writeJiraVersion('https://gravitee.atlassian.net/rest/api/3/version', 'POST', { name: next, projectId });
      }
      created = next;
    }
  } catch (error) {
    return { skipped: false, released, created, unclosed, error };
  }
  return { skipped: false, released, created, unclosed };
}
