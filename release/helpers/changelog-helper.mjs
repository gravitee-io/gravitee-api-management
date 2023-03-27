/**
 * Get the Ascii doc formatted changelog for input issues
 *
 * @param issues {Array<{id: string, fields: Array<{customfield_10115: string,summary: string}>}>}
 */
export function getChangelogFor(issues) {
  return issues
    .sort((issue1, issue2) => {
      // if null or undefined, put it at the end
      if (!issue1.fields.customfield_10115) {
        return 1;
      } else if (!issue2.fields.customfield_10115) {
        return -1;
      } else {
        return issue1.fields.customfield_10115 - issue2.fields.customfield_10115;
      }
    })
    .map((issue) => {
      const githubLink = `https://github.com/gravitee-io/issues/issues/${issue.fields.customfield_10115}`;
      return `* ${issue.fields.summary} ${githubLink}[#${issue.fields.customfield_10115}]`;
    })
    .join('\n');
}
