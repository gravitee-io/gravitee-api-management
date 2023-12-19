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

import {
  stopApi,
  deleteApi,
  deployApi,
  importSwaggerApi,
  startApi,
  updateApi,
  stopV4Api,
  deleteV4Api,
} from '@commands/management/api-management-commands';
import { closePlan, createPlan, publishPlan } from '@commands/management/api-plan-management-commands';
import { ApiImportFakers } from '@fakers/api-imports';
import { ADMIN_USER, API_PUBLISHER_USER } from '@fakers/users/users';
import { ApiImport, ImportSwaggerDescriptorEntity } from '@model/api-imports';
import faker from '@faker-js/faker';
import { Api } from '@model/apis';

declare global {
  namespace Cypress {
    interface Chainable<Subject> {
      teardownApi(api: ApiImport | Api): void;
      teardownV4Api(apiId: string): void;
      createAndStartApiFromSwagger(swaggerImport: string, attributes?: Partial<ImportSwaggerDescriptorEntity>): any;
      callGateway(
        contextPath: string,
        checkConditionFn?: (response: Cypress.Response<any>) => boolean,
        maxRetries?: number,
        retryDelay?: number,
      ): Cypress.Chainable<Cypress.Response<any>>;
    }
  }
}
export {};

Cypress.Commands.add('callGateway', (contextPath, checkConditionFn?, maxRetries = 20, retryDelay = 1500) => {
  const defaultCheckFunction = (response: Cypress.Response<any>) => response.status === 200;
  const isResponseValid = checkConditionFn || defaultCheckFunction;
  const url = `${Cypress.env('gatewayServer')}${contextPath}`;
  const sendRequest = (retriesLeft: number) => {
    cy.log(`Calling gateway: ${url} - Retry ${maxRetries - retriesLeft} of ${maxRetries}`);
    cy.request({ url, failOnStatusCode: false }).then((response) => {
      if (isResponseValid(response)) {
        return response;
      }
      if (retriesLeft > 0) {
        cy.wait(retryDelay);
        sendRequest(retriesLeft - 1);
      } else {
        throw new Error(
          `API did not return the expected result within the allowed retries. \nFunction used to check the response:\n ${checkConditionFn.toString()}`,
        );
      }
    });
  };
  return sendRequest(maxRetries);
});

Cypress.Commands.add('teardownApi', (api) => {
  cy.log(`----- Removing API "${api.name}" -----`);
  const plans: any[] = [];
  if (api.plans) {
    plans.push(...api.plans);
  }
  cy.log(`Number of plans: ${plans.length}`);
  if (plans.length > 0) {
    plans.forEach((plan) => closePlan(ADMIN_USER, api.id, plan.id).ok());
  }
  stopApi(ADMIN_USER, api.id);
  deleteApi(ADMIN_USER, api.id).noContent();
});

Cypress.Commands.add('teardownV4Api', (apiId) => {
  cy.log(`----- Removing V4 API (${apiId}) -----`);
  cy.log(`Close all plans attached to that API`);
  cy.log(`Stop API`);
  stopV4Api(ADMIN_USER, apiId);
  deleteV4Api(ADMIN_USER, apiId, true).noContent();
});

Cypress.Commands.add('createAndStartApiFromSwagger', (swaggerImport: string, attributes?) => {
  importSwaggerApi(API_PUBLISHER_USER, swaggerImport, attributes).then((response) => {
    if (response.status !== 201) return response;
    let api: ApiImport = response.body;
    const fakePlan = ApiImportFakers.plan();
    const name = `swagger_${faker.datatype.number()}`;
    api.name = name;
    api.proxy.virtual_hosts = [{ path: `/${name}` }];
    api.proxy.groups[0].endpoints.forEach((_value, index) => {
      api.proxy.groups[0].endpoints[index].target = `${Cypress.env('localPetstore_v2')}`;
    });
    // @ts-ignore
    updateApi(API_PUBLISHER_USER, api.id, api);
    // @ts-ignore
    return createPlan(API_PUBLISHER_USER, api.id, fakePlan)
      .its('body')
      .then((plan) => {
        publishPlan(API_PUBLISHER_USER, api.id, plan.id);
        return deployApi(API_PUBLISHER_USER, api.id)
          .its('body')
          .then((api) => {
            return startApi(API_PUBLISHER_USER, api.id).then(() => {
              return api;
            });
          });
      });
  });
});
