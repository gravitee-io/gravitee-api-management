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

interface GatewayRequestOptions {
  timeBetweenRetries: number;
  failAfterMs: number;
  validWhen?: Function;
}

export function requestGateway(
  request: Partial<Cypress.RequestOptions>,
  options?: Partial<GatewayRequestOptions>,
): Cypress.Chainable<Cypress.Response<any>> {
  options = <GatewayRequestOptions>{
    timeBetweenRetries: 200,
    failAfterMs: 7000,
    validWhen: (response) => {
      return response.status === 200;
    },
    ...options,
  };
  request = <Cypress.RequestOptions>{
    failOnStatusCode: false,
    ...request,
  };

  if (options.failAfterMs > 0) {
    return cy.request(request).then((response) => {
      if (options.validWhen(response)) {
        return cy.wrap(response);
      }
      cy.wait(options.timeBetweenRetries);
      options.failAfterMs -= options.timeBetweenRetries;

      return requestGateway(request, options);
    });
  }

  throw new Error(`Failed to request ${request.url} after specified delay`);
}
