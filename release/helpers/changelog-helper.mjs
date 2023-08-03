/**
 * Get the Ascii doc formatted changelog for input issues
 *
 * @param issues {Array<{id: string, githubIssue: string, fields: Array<{summary: string}>}>}
 */
export function getChangelogFor(issues) {
  return issues
    .sort((issue1, issue2) => {
      // if null or undefined, put it at the end
      if (!issue1.githubIssue) {
        return 1;
      } else if (!issue2.githubIssue) {
        return -1;
      } else {
        return issue1.githubIssue - issue2.githubIssue;
      }
    })
    .map((issue) => {
      if (issue.githubIssue && issue.githubIssue !== '') {
        const githubLink = `https://github.com/gravitee-io/issues/issues/${issue.githubIssue}`;
        return `* ${issue.summary} [#${issue.githubIssue}](${githubLink})`;
      }

      return `* ${issue.summary}`;

    })
    .join('\n');
}
