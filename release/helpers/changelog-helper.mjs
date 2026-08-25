export const ComponentTypes = ['Gateway', 'Management API', 'Console', 'Portal', 'Helm Charts'];

export const ProductSections = {
  ESM: 'Event Stream Management',
  AIAM: 'AI Agent Management',
  OBS: 'Observability',
  AUTHZ: 'Authorization Management',
};

// Boards whose tickets belong to a component section that already exists rather than to one of their
// own: the APIM 'Portal' component and the Portal board are the same product. A board appears in at
// most one of ProductSections and BoardComponents.
export const BoardComponents = { PORTAL: 'Portal' };

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
 * @param issues {Array<{id: string, githubIssue: string, summary: string, components: Array<{name: string}>, type: string>}
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

function hasComponentSection(issue) {
  return Boolean(BoardComponents[issue.project]);
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
 * Get the tickets for a given component and a given type of tickets. If type is 'Other', it will return
 * tickets with no known component from the boards that have neither a product section nor a mapped
 * component section.
 *
 * <p>A component takes the ticket whichever board raised it, and a board mapped in BoardComponents adds
 * its componentless tickets to that component's section. Only 'Other' is narrowed: a board with a
 * section of its own has its buckets replaced, so catching its tickets here too would list them twice.
 *
 * @param issues {Array<{id: string, project: string, githubIssue: string, summary: string, components: Array<{name: string}>, type: string>}
 * @param componentType {ComponentTypes | 'Other'}
 * @param ticketType {'Public Bug' | 'Public Improvement'}
 */
export function getTicketsFor(issues, componentType, ticketType) {
  const ticketsOfType = issues.filter((issue) => issue.type === ticketType);
  const componentsTickets =
    componentType === 'Other'
      ? ticketsOfType.filter((issue) => !hasProductSection(issue) && !hasComponentSection(issue) && carriesNoComponent(issue))
      : ticketsOfType.filter(
          (issue) =>
            issue.components.some((cmp) => cmp.name === componentType) ||
            (BoardComponents[issue.project] === componentType && carriesNoComponent(issue)),
        );

  return headedSection(componentType, componentsTickets);
}

/**
 * Get the tickets of one product board that no component section already covers, headed by the product's
 * name. Only boards returned by productKeysOf have a product section; any other key renders nothing.
 *
 * @param issues {Array<{id: string, project: string, githubIssue: string, summary: string, components: Array<{name: string}>, type: string>}
 * @param projectKey {string}
 * @param ticketType {'Public Bug' | 'Public Improvement'}
 */
export function getTicketsForProduct(issues, projectKey, ticketType) {
  const title = ProductSections[projectKey];
  if (!title) {
    return undefined;
  }
  const productTickets = issues.filter((issue) => issue.project === projectKey && issue.type === ticketType && carriesNoComponent(issue));

  return headedSection(title, productTickets);
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

/**
 * The sections of one changelog block as rendered strings, in display order: the long-standing
 * component sections first, then one section per product board present, then Other last — the catch-all
 * must not sit between the named sections.
 *
 * @param issues {Array<{id: string, project: string, githubIssue: string, summary: string, components: Array<{name: string}>, type: string>}
 * @param ticketType {'Public Bug' | 'Public Improvement'}
 * @returns {Array<string>}
 */
export function getTicketSections(issues, ticketType) {
  return [
    ...ComponentTypes.map((componentType) => getTicketsFor(issues, componentType, ticketType)),
    ...productKeysOf(issues).map((projectKey) => getTicketsForProduct(issues, projectKey, ticketType)),
    getTicketsFor(issues, 'Other', ticketType),
  ].filter(Boolean);
}
