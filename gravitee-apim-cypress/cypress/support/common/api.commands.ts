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

import { stopApi, deleteApi } from '@commands/management/api-management-commands';
import { closePlan } from '@commands/management/api-plan-management-commands';
import { ADMIN_USER } from '@fakers/users/users';
import { ApiImport } from '@model/api-imports';

declare global {
  namespace Cypress {
    interface Chainable<Subject> {
      teardownApi(api: ApiImport): void;
    }
  }
}
export {};

Cypress.Commands.add('teardownApi', (api) => {
  cy.log(`----- Removing API "${api.name}" -----`);
  closePlan(ADMIN_USER, api.id, api.plans[0].id).ok();
  stopApi(ADMIN_USER, api.id).noContent();
  deleteApi(ADMIN_USER, api.id).noContent();
});
