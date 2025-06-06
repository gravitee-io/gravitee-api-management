/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

declare global {
  namespace Cypress {
    interface Chainable<Subject> {
      checkAccessibility(): void;
    }
  }
}

Cypress.Commands.add('checkAccessibility', () => {
  cy.injectAxe({ axeCorePath: './node_modules/axe-core/axe.min.js' });
  cy.checkA11y(
    null,
    {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa'],
      },
    },
    terminalLog,
  );
});

function terminalLog(violations) {
  cy.task(
    'log',
    `${violations.length} accessibility violation${violations.length === 1 ? '' : 's'} ${
      violations.length === 1 ? 'was' : 'were'
    } detected`,
  );
  violations.forEach((violation) => {
    cy.log(`${violation.id} - ${violation.description}`);
    violation.nodes.forEach((node) => {
      cy.log(`Node: ${node.target}`);
    });
  });

  // pluck specific keys to keep the table readable
  const violationData = violations.map(({ id, impact, description, nodes }) => ({
    id,
    impact,
    description,
    nodes: nodes.length,
  }));

  cy.task('table', violationData);
}
