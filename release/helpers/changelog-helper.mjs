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

/**
 * Get the tickets for a given component and a given type of tickets. If type is 'Other', it will return tickets that are not in the ComponentTypes list.
 *
 * @param issues {Array<{id: string, fields: Array<{customfield_10115: string,summary: string}>}>}
 * @param componentType {ComponentTypes | 'Other'}
 * @param ticketType {'Public Bug' | 'Public Improvement'}
 */
export function getTicketsFor(issues, componentType, ticketType) {
  let componentsTickets;
  if (componentType === 'Other') {
    componentsTickets = issues.filter(
      (issue) => issue.fields.issuetype.name === ticketType && issue.fields.components.every((cmp) => !ComponentTypes.includes(cmp.name)),
    );
  } else {
    componentsTickets = issues.filter(
      (issue) => issue.fields.issuetype.name === ticketType && issue.fields.components.some((cmp) => cmp.name === componentType),
    );
  }
  let ticketsForComponent;
  if (componentsTickets.length > 0) {
    ticketsForComponent = `==== ${componentType}

${getChangelogFor(componentsTickets)}

`;
  }

  return ticketsForComponent;
}
