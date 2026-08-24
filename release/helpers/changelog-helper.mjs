export const ComponentTypes = ['Gateway', 'Management API', 'Console', 'Portal', 'Helm Charts'];

const ProductSections = {
  ESM: 'Event Stream Management',
  AIAM: 'AI Agent Management',
  PORTAL: 'Portal',
  OBS: 'Observability',
  AUTHZ: 'Authorization Management',
};

export const ChangelogSections = [
  {
    ticketType: 'Public Bug',
    title: 'Bug Fixes',
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
        // Must escape '[' to avoid being considered as a link in GitBook
        return `* ${issue.summary.replaceAll('[', '\\[')} [#${issue.githubIssue}](${githubLink})`;
      }

      return `* ${issue.summary.replaceAll('[', '\\[')}`;
    })
    .join('\n');
}

function hasProductSection(issue) {
  return Boolean(ProductSections[issue.project]);
}

function headedSection(title, tickets) {
  if (tickets.length === 0) {
    return undefined;
  }

  return `**${title}**

${getChangelogFor(tickets)}

`;
}

function carriesNoComponent(issue) {
  return issue.components.every((cmp) => !ComponentTypes.includes(cmp.name));
}

/**
 * Get the tickets for a given component and a given type of tickets. If type is 'Other', it will return tickets with no known component from the boards that have no product section.
 *
 * <p>A component takes the ticket whichever board raised it. Only 'Other' is narrowed: a board with a
 * product section has that bucket replaced by the section, so catching its tickets here too would list
 * them twice.
 *
 * @param issues {Array<{id: string, project: string, githubIssue: string, summary: string, components: Array<string>, type: string>}
 * @param componentType {ComponentTypes | 'Other'}
 * @param ticketType {'Public Bug' | 'Public Improvement'}
 */
export function getTicketsFor(issues, componentType, ticketType) {
  const ticketsOfType = issues.filter((issue) => issue.type === ticketType);
  const componentsTickets =
    componentType === 'Other'
      ? ticketsOfType.filter((issue) => !hasProductSection(issue) && carriesNoComponent(issue))
      : ticketsOfType.filter((issue) => issue.components.some((cmp) => cmp.name === componentType));

  return headedSection(componentType, componentsTickets);
}

/**
 * Get the tickets of one product board that no component section already covers, headed by the product's name.
 *
 * @param issues {Array<{id: string, project: string, githubIssue: string, summary: string, components: Array<string>, type: string>}
 * @param projectKey {string}
 * @param ticketType {'Public Bug' | 'Public Improvement'}
 */
export function getTicketsForProduct(issues, projectKey, ticketType) {
  const productTickets = issues.filter((issue) => issue.project === projectKey && issue.type === ticketType && carriesNoComponent(issue));

  return headedSection(ProductSections[projectKey], productTickets);
}

/**
 * The product boards these issues came from, in the order they are declared rather than the order the
 * tickets happened to arrive.
 *
 * @param issues {Array<{project: string}>}
 * @returns {Array<string>}
 */
export function productKeysOf(issues) {
  const present = new Set(issues.filter(hasProductSection).map((issue) => issue.project));

  return Object.keys(ProductSections).filter((projectKey) => present.has(projectKey));
}
