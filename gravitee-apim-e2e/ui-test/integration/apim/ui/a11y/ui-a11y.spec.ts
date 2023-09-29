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
import 'cypress-axe';

const axeOptions = {};
function terminalLog(violations) {
  cy.task(
    'log',
    `${violations.length} accessibility violation${violations.length === 1 ? '' : 's'} ${
      violations.length === 1 ? 'was' : 'were'
    } detected`,
  );
  // pluck specific keys to keep the table readable
  const violationData = violations.map(({ id, impact, description, nodes }) => ({
    id,
    impact,
    description,
    nodes: nodes.length,
  }));

  cy.task('table', violationData);
}

describe('Accessibility test', () => {
  // Here we use a beforeEach and an after each to reset properly all the cookie we want
  // otherwise we are sometimes redirected or face XRCF issues
  beforeEach(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
  });

  it(`Check login page on path #!/login`, () => {
    cy.visit('#!/login');
    cy.injectAxe(axeOptions);
    cy.wait(3000);
    cy.checkA11y(null, null, terminalLog);
  });
});
