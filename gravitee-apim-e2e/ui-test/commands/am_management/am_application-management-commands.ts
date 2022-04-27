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

import { NewApplication } from '@model/am_applications';

export function am_createApplication(token: string, name: string, domainId: string, attributes?: Partial<NewApplication>) {
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('am_managementAPI')}/domains/${domainId}/applications`,
    auth: { bearer: token },
    body: {
      name,
      type: 'SERVICE',
      ...attributes,
    },
  });
}

export function am_deleteApplication(token: string, domainId: string, applicationId: string) {
  return cy.request({
    method: 'DELETE',
    url: `${Cypress.env('am_managementAPI')}/domains/${domainId}/applications/${applicationId}`,
    auth: {
      bearer: token,
    },
  });
}

export function am_patchApplication(token: string, domainId: string, applicationId: string, body: any) {
  return cy.request({
    method: 'PATCH',
    url: `${Cypress.env('am_managementAPI')}/domains/${domainId}/applications/${applicationId}`,
    auth: { bearer: token },
    body,
  });
}
