/**
 * Get the Jira version associated to the given version name
 * @param versionName {string} The version name
 * @returns {Promise<{
 *   self: string,
 *   id: string,
 *   name: string,
 *   archived: boolean,
 *   released: boolean,
 *   releaseDate: string,
 *   userReleaseDate: string,
 *   projectId: number
 * }>}
 */
export function getJiraVersion(versionName) {
  const token = process.env.JIRA_TOKEN;

  return fetch(`https://gravitee.atlassian.net/rest/api/3/project/APIM/versions`, {
    method: 'GET',
    headers: {
      Authorization: `Basic ${token}`,
      Accept: 'application/json',
    },
  })
    .then((response) => response.json())
    .then((versions) => versions.find((version) => version.name === versionName));
}

/**
 * Get all Jira issues associated to the given version id
 * @param versionId {string} The version id
 * @returns {Promise<Array<{id: string, key: string, githubIssue: string, summary: string, components: Array<string>}>>}
 */
export async function getJiraIssuesOfVersion(versionId) {
  const token = process.env.JIRA_TOKEN;

    const issuesFromJira = await fetch('https://gravitee.atlassian.net/rest/api/3/search/jql', {
        method: 'POST',
        headers: {
            Authorization: `Basic ${token}`,
            Accept: 'application/json',
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            jql: `project = APIM AND fixVersion = "${versionId}"`,
            fields: ['issuetype', 'summary', 'components', 'customfield_10115'],
        }),
    })
        .then((response) => response.json())
        .then((body) => body.issues);

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
      githubIssue: issue.fields.customfield_10115,
      summary: issue.fields.summary,
      components: issue.fields.components,
      type: issue.fields.issuetype.name,
    }));

  // For each issue with empty githubIssue field, get the remote link and extract the GitHub issue number
  for (const issue of issues.filter((issue) => !issue.githubIssue)) {
    const remoteLinks = await fetch(`https://gravitee.atlassian.net/rest/api/3/issue/${issue.key}/remotelink`, {
      method: 'GET',
      headers: {
        Authorization: `Basic ${token}`,
        Accept: 'application/json',
      },
    }).then((response) => response.json());

    const githubIssue = remoteLinks.find((remoteLink) => remoteLink.object.url.includes('https://github.com/gravitee-io/issues/issues/'));
    if (githubIssue) {
      issue.githubIssue = githubIssue.object.url.replace('https://github.com/gravitee-io/issues/issues/', '');
    }
  }

  return issues;
}
