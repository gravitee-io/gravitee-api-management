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
      httpStatus(statusCode: number): Chainable<Subject>;
      ok(): Chainable<Subject>;
      created(): Chainable<Subject>;
      noContent(): Chainable<Subject>;
      badRequest(): Chainable<Subject>;
      unauthorized(): Chainable<Subject>;
      forbidden(): Chainable<Subject>;
      notFound(): Chainable<Subject>;
      serviceUnavailable(): Chainable<Subject>;
      retryRequest(url: string, options: {}): Chainable<Subject>;
    }
  }
}
export {};

Cypress.Commands.add(
  'httpStatus',
  {
    prevSubject: true,
  },
  (subject, statusCode: number) => {
    expect(subject.status).to.equal(statusCode);
    return subject;
  },
);

Cypress.Commands.add(
  'ok',
  {
    prevSubject: true,
  },
  (subject) => {
    expect(subject.status).to.equal(200);
    return subject;
  },
);

Cypress.Commands.add(
  'created',
  {
    prevSubject: true,
  },
  (subject) => {
    expect(subject.status).to.equal(201);
    return subject;
  },
);

Cypress.Commands.add(
  'noContent',
  {
    prevSubject: true,
  },
  (subject) => {
    expect(subject.status).to.equal(204);
    return subject;
  },
);

Cypress.Commands.add(
  'badRequest',
  {
    prevSubject: true,
  },
  (subject) => {
    expect(subject.status).to.equal(400);
    return subject;
  },
);

Cypress.Commands.add(
  'unauthorized',
  {
    prevSubject: true,
  },
  (subject) => {
    expect(subject.status).to.equal(401);
    return subject;
  },
);

Cypress.Commands.add(
  'notFound',
  {
    prevSubject: true,
  },
  (subject) => {
    expect(subject.status).to.equal(404);
    return subject;
  },
);

Cypress.Commands.add(
  'serviceUnavailable',
  {
    prevSubject: true,
  },
  (subject) => {
    expect(subject.status).to.equal(503);
    return subject;
  },
);

Cypress.Commands.add('retryRequest', (url, options) => {
  Cypress._.defaults(options, {
    method: 'GET',
    body: null,
    headers: {
      'content-type': 'application/json',
    },
    retries: 0,
    function: false,
    timeout: 0,
  });
  let retriesCounter = 0;
  function makeRequest() {
    retriesCounter += 1;
    cy.request({
      method: options.method || 'GET',
      url: url,
      headers: options.headers,
      body: options.body,
      failOnStatusCode: false,
    }).then((response) => {
      try {
        if (options.function) {
          options.function(response);
        }
        return cy.wrap(response);
      } catch (err) {
        if (!options.function) {
          throw err;
        }
        if (retriesCounter >= options.retries) {
          throw new Error(`Failed to request ${url} after ${options.retries} retries`);
        }
        cy.wait(options.timeout);
        return makeRequest();
      }
    });
  }
  makeRequest();
});
