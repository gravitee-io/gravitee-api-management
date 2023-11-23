export const ComponentTypes = ['Gateway', 'Management API', 'Console', 'Portal', 'Helm Charts'];
export const ChangelogSections = [
  {
    ticketType: 'Public Bug',
    title: 'BugFixes',
  },
  {
    ticketType: 'Public Improvement',
    title: 'Improvements',
  },
];

/**
 * Get the Ascii doc formatted changelog for input issues
 *
 * @param issues {Array<{id: string, githubIssue: string, summary: string, components: Array<string>, type: string>}
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

/**
 * Get the tickets for a given component and a given type of tickets. If type is 'Other', it will return tickets that are not in the ComponentTypes list.
 *
 * @param issues {Array<{id: string, githubIssue: string, summary: string, components: Array<string>, type: string>}
 * @param componentType {ComponentTypes | 'Other'}
 * @param ticketType {'Public Bug' | 'Public Improvement'}
 */
export function getTicketsFor(issues, componentType, ticketType) {
  let componentsTickets;
  if (componentType === 'Other') {
    componentsTickets = issues.filter(
      (issue) => issue.type === ticketType && issue.components.every((cmp) => !ComponentTypes.includes(cmp.name)),
    );
  } else {
    componentsTickets = issues.filter((issue) => issue.type === ticketType && issue.components.some((cmp) => cmp.name === componentType));
  }
  let ticketsForComponent;
  if (componentsTickets.length > 0) {
    ticketsForComponent = `**${componentType}**

${getChangelogFor(componentsTickets)}

`;
  }

  return ticketsForComponent;
}
