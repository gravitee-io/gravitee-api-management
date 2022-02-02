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
import { BasicAuthentication } from '@model/users';
import { ApiPageType } from '@model/apis';

class ApiPagesQueryParams {
  root?: boolean;
  type?: ApiPageType;
  parent?: string;
}

export function getPages(auth: BasicAuthentication, apiId: string, params = new ApiPagesQueryParams()) {
  return cy.request({
    method: 'GET',
    url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/apis/${apiId}/pages`,
    auth,
    failOnStatusCode: false,
    qs: params,
  });
}

export function getPage(auth: BasicAuthentication, apiId: string, pageId: string) {
  return cy.request({
    method: 'GET',
    url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/apis/${apiId}/pages/${pageId}`,
    auth,
    failOnStatusCode: false,
  });
}

export function deletePage(auth: BasicAuthentication, apiId: string, pageId: string) {
  return cy.request({
    method: 'DELETE',
    url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/apis/${apiId}/pages/${pageId}`,
    auth,
    failOnStatusCode: false,
  });
}
