/**
 * The Jira projects a release can draw tickets from.
 *
 * <p>A release note used to come from APIM alone. The modules that ship alongside it now have boards of
 * their own, and a ticket fixed in one of them belongs in the same changelog as the APIM ones. Override
 * with JIRA_PROJECTS (comma separated) to add a board without a code change.
 */
function jiraProjects() {
  return (process.env.JIRA_PROJECTS ?? 'APIM,ESM,AIAM,PORTAL,OBS,AUTHZ,FOUND,BX')
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
 * <p>A project that does not release this version simply has none. That is normal rather than an error —
 * only a version found nowhere means there is nothing to write.
 *
 * @param versionName {string} The version name
 * @param projectKeys {Array<string>} The projects to check; defaults to JIRA_PROJECTS
 * @returns {Promise<Array<string>>}
 */
export async function getJiraVersions(versionName, projectKeys = jiraProjects()) {
  const found = await Promise.all(
    projectKeys.map(async (projectKey) => {
      try {
        const versions = await fetch(`https://gravitee.atlassian.net/rest/api/3/project/${projectKey}/versions`, {
          method: 'GET',
          headers: jiraHeaders(),
        }).then((response) => response.json());

        if (!Array.isArray(versions)) {
          // A project the token cannot read answers with an error object rather than a list. Say so and
          // carry on: one unreadable board must not cost the release its whole changelog.
          console.warn(`⚠️  Could not read versions of project ${projectKey}, skipping it.`);
          return undefined;
        }

        return versions.some((candidate) => candidate.name === versionName) ? projectKey : undefined;
      } catch (error) {
        console.warn(`⚠️  Could not read versions of project ${projectKey}: ${error.message}`);
        return undefined;
      }
    }),
  );

  return found.filter(Boolean);
}

/**
 * Every public ticket fixed in the given version, across the given projects.
 *
 * <p>Matched on the version name, scoped to those projects: an issue in a project can only ever carry that
 * project's own version, so `project IN (...) AND fixVersion = "name"` cannot cross-match another board's
 * version even when both share the name. The results are paged through to the end: the search endpoint
 * returns a page at a time, and a release spanning several boards passes that page size easily.
 *
 * @param projectKeys {Array<string>}
 * @param versionName {string}
 * @returns {Promise<Array<{key: string, githubIssue: string, summary: string, components: Array<object>, type: string}>>}
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
      // An error-shaped response must not read as "zero issues": that would write and commit an empty
      // changelog as if the release genuinely shipped nothing public.
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
      githubIssue: issue.fields.customfield_10115,
      summary: issue.fields.summary,
      components: issue.fields.components ?? [],
      type: issue.fields.issuetype.name,
    }));

  // Boards other than APIM never fill in the GitHub issue field, so this fallback runs for most of a
  // non-APIM batch rather than the odd straggler — fetched concurrently so it no longer costs one
  // round-trip per ticket in series.
  await Promise.all(
    issues
      .filter((issue) => !issue.githubIssue)
      .map(async (issue) => {
        const remoteLinks = await fetch(`https://gravitee.atlassian.net/rest/api/3/issue/${issue.key}/remotelink`, {
          method: 'GET',
          headers: jiraHeaders(),
        }).then((response) => response.json());

        if (!Array.isArray(remoteLinks)) {
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
