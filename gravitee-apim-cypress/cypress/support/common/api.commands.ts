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
  createApi,
  updateApi,
} from '@commands/management/api-management-commands';
import { closePlan, createPlan, publishPlan } from '@commands/management/api-plan-management-commands';
import { ApiImportFakers } from '@fakers/api-imports';
import { ADMIN_USER, API_PUBLISHER_USER } from '@fakers/users/users';
import { ApiImport, ImportSwaggerDescriptorEntity, ImportSwaggerDescriptorEntityType } from '@model/api-imports';
import * as faker from 'faker';

declare global {
  namespace Cypress {
    interface Chainable<Subject> {
      teardownApi(api: ApiImport): void;
      createAndStartApiFromSwagger(swaggerImport: string, attributes?: Partial<ImportSwaggerDescriptorEntity>): any;
    }
  }
}
export {};

Cypress.Commands.add('teardownApi', (api) => {
  cy.log(`----- Removing API "${api.name}" -----`);
  cy.log(`Number of plans: ${api.plans.length}`);
  if (api.plans.length > 0) {
    api.plans.forEach((plan) => closePlan(ADMIN_USER, api.id, plan.id).ok());
  }
  stopApi(ADMIN_USER, api.id).noContent();
  deleteApi(ADMIN_USER, api.id).noContent();
});

Cypress.Commands.add('createAndStartApiFromSwagger', (swaggerImport: string, attributes?) => {
  if (swaggerImport.startsWith('http')) attributes.type = ImportSwaggerDescriptorEntityType.URL;
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
    updateApi(API_PUBLISHER_USER, api.id, api);
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
