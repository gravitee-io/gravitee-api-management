/**
 * The Jira projects a release can draw tickets from.
 *
 * <p>A release note used to come from APIM alone. The modules that ship alongside it now have boards of
 * their own, and a ticket fixed in one of them belongs in the same changelog as the APIM ones. Override
 * with JIRA_PROJECTS (comma separated) to add a board without a code change.
 */
export const JiraProjects = (process.env.JIRA_PROJECTS ?? 'APIM,ESM,AIAM,PORTAL,OBS,AUTHZ')
  .split(',')
  .map((key) => key.trim())
  .filter(Boolean);

function jiraHeaders() {
  return {
    Authorization: `Basic ${process.env.JIRA_TOKEN}`,
    Accept: 'application/json',
  };
}

/**
 * The projects that have a version named `versionName`.
 *
 * <p>A project that does not release this version simply has none. That is normal rather than an error —
 * only a version found nowhere means there is nothing to write.
 *
 * @param versionName {string} The version name
 * @returns {Promise<Array<{projectKey: string, name: string}>>}
 */
export async function getJiraVersions(versionName) {
  const found = await Promise.all(
    JiraProjects.map(async (projectKey) => {
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

        const version = versions.find((candidate) => candidate.name === versionName);
        return version === undefined ? undefined : { projectKey, name: version.name };
      } catch (error) {
        console.warn(`⚠️  Could not read versions of project ${projectKey}: ${error.message}`);
        return undefined;
      }
    }),
  );

  return found.filter(Boolean);
}

/**
 * Every public ticket fixed in the given versions, across the projects they belong to.
 *
 * <p>Matched on the version name, scoped to those projects: an issue in a project can only ever carry that
 * project's own version, so `project IN (...) AND fixVersion = "name"` cannot cross-match another board's
 * version even when both share the name. The results are paged through to the end: the search endpoint
 * returns a page at a time, and a release spanning several boards passes that page size easily.
 *
 * @param versions {Array<{projectKey: string, name: string}>}
 * @returns {Promise<Array<{key: string, project: string, githubIssue: string, summary: string, components: Array<object>, type: string}>>}
 */
export async function getJiraIssuesOfVersions(versions) {
  if (versions.length === 0) {
    return [];
  }

  const projects = [...new Set(versions.map((version) => version.projectKey))].join(', ');
  const jql = `project IN (${projects}) AND fixVersion = "${versions[0].name}"`;

  const issuesFromJira = [];
  let nextPageToken;
  do {
    const page = await fetch('https://gravitee.atlassian.net/rest/api/3/search/jql', {
      method: 'POST',
      headers: { ...jiraHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({
        jql,
        maxResults: 100,
        fields: ['issuetype', 'summary', 'components', 'project', 'customfield_10115'],
        ...(nextPageToken ? { nextPageToken } : {}),
      }),
    }).then((response) => response.json());

    issuesFromJira.push(...(page.issues ?? []));
    nextPageToken = page.nextPageToken;
  } while (nextPageToken);

  // Filter out issues that are not public bugs or public security issues
  const issues = issuesFromJira
    .filter(
      (issue) =>
        issue.fields.issuetype.name === 'Public Bug' ||
        issue.fields.issuetype.name === 'Public Security' ||
        issue.fields.issuetype.name === 'Public Improvement',
    )
    .map((issue) => ({
      key: issue.key,
      project: issue.fields.project?.key ?? issue.key.split('-')[0],
      githubIssue: issue.fields.customfield_10115,
      summary: issue.fields.summary,
      components: issue.fields.components ?? [],
      type: issue.fields.issuetype.name,
    }));

  // For each issue with empty githubIssue field, get the remote link and extract the GitHub issue number
  for (const issue of issues.filter((issue) => !issue.githubIssue)) {
    const remoteLinks = await fetch(`https://gravitee.atlassian.net/rest/api/3/issue/${issue.key}/remotelink`, {
      method: 'GET',
      headers: jiraHeaders(),
    }).then((response) => response.json());

    if (!Array.isArray(remoteLinks)) {
      continue;
    }
    const githubIssue = remoteLinks.find((remoteLink) => remoteLink.object.url.includes('https://github.com/gravitee-io/issues/issues/'));
    if (githubIssue) {
      issue.githubIssue = githubIssue.object.url.replace('https://github.com/gravitee-io/issues/issues/', '');
    }
  }

  return issues;
}
