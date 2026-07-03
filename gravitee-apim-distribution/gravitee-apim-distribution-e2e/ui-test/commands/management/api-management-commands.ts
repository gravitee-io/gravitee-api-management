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
import { Api, ApiLifecycleState, ApiMember, UpdateApiEntity } from '@model/apis';
import {
  ApiImport,
  ImportSwaggerDescriptorEntity,
  ImportSwaggerDescriptorEntityFormat,
  ImportSwaggerDescriptorEntityType,
} from '@model/api-imports';
import { BasicAuthentication } from '@model/users';
import { ProcessSubscriptionEntity } from '@model/api-subscriptions';
import { requestGateway } from 'ui-test/support/common/http.commands';
import { ApiImportEntity } from '@gravitee/fixtures/management/ApisFaker';

export function createApi(auth: BasicAuthentication, body: Api, failOnStatusCode = false) {
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis`,
    body,
    auth,
    failOnStatusCode,
  });
}

export function publishApi(auth: BasicAuthentication, createdApi: Api, failOnStatusCode = false) {
  const apiToPublish = {
    ...createdApi,
    lifecycle_state: ApiLifecycleState.PUBLISHED,
  };
  delete apiToPublish.id;
  delete apiToPublish.state;
  delete apiToPublish.created_at;
  delete apiToPublish.updated_at;
  delete apiToPublish.owner;
  delete apiToPublish.contextPath;
  return cy.request({
    method: 'PUT',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${createdApi.id}`,
    body: apiToPublish,
    auth,
    failOnStatusCode,
  });
}

export function deleteApi(auth: BasicAuthentication, apiId: string, failOnStatusCode = false) {
  cy.log(`Deleting API with id: ${apiId}`);
  return cy.request({
    method: 'DELETE',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}`,
    auth,
    failOnStatusCode,
  });
}

export function deleteV4Api(auth: BasicAuthentication, apiId: string, closePlans: boolean, failOnStatusCode = false) {
  cy.log(`Deleting API with id: ${apiId}`);
  return cy.request({
    method: 'DELETE',
    url: `${Cypress.env('managementApi')}/management/v2/environments/DEFAULT/apis/${apiId}?closePlans=${closePlans}`,
    auth,
    failOnStatusCode,
  });
}

export function deployApi(auth: BasicAuthentication, apiId: string, failOnStatusCode = false) {
  cy.log(`Deploying API with id: ${apiId}`);
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/deploy`,
    auth,
    failOnStatusCode,
  });
}

export function startApi(auth: BasicAuthentication, apiId: string, failOnStatusCode = false) {
  cy.log(`Starting API with id: ${apiId}`);
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}?action=START`,
    auth,
    failOnStatusCode,
  });
}

export function stopApi(auth: BasicAuthentication, apiId: string, failOnStatusCode = false) {
  cy.log(`Stopping API with id: ${apiId}`);
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}?action=STOP`,
    auth,
    failOnStatusCode,
  });
}

export function stopV4Api(auth: BasicAuthentication, apiId: string, failOnStatusCode = false) {
  cy.log(`Stopping API with id: ${apiId}`);
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}/management/v2/environments/DEFAULT/apis/${apiId}/_stop`,
    auth,
    failOnStatusCode,
  });
}

export function importCreateApi(auth: BasicAuthentication, body: ApiImport) {
  cy.log(`Creating API with name: ${body.name}`);
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/import`,
    body,
    auth,
    failOnStatusCode: false,
  });
}

export function importSwaggerApi(auth: BasicAuthentication, swaggerImport: string, attributes?: Partial<ImportSwaggerDescriptorEntity>) {
  cy.log(`Importing Swagger API`);
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/import/swagger`,
    qs: { definitionVersion: '2.0.0' },
    body: {
      payload: swaggerImport,
      ...attributes,
    },
    auth,
    failOnStatusCode: false,
  });
}

export function importUpdateApi(auth: BasicAuthentication, apiId: string, body: ApiImport) {
  cy.log(`Importing API with id: ${apiId}`);
  return cy.request({
    method: 'PUT',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/import`,
    body,
    auth,
    failOnStatusCode: false,
  });
}

export function exportApi(auth: BasicAuthentication, apiId: string) {
  cy.log(`Exporting API with id: ${apiId}`);
  return cy.request({
    method: 'GET',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/export`,
    auth,
    failOnStatusCode: false,
  });
}

export function getApiById(auth: BasicAuthentication, apiId: string) {
  cy.log(`Getting API with id: ${apiId}`);
  return cy.request({
    method: 'GET',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}`,
    auth,
    failOnStatusCode: false,
  });
}

export function getApiMetadata(auth: BasicAuthentication, apiId: string) {
  cy.log(`Getting metadata for API with id: ${apiId}`);
  return cy.request({
    method: 'GET',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/metadata`,
    auth,
    failOnStatusCode: false,
  });
}

export function addMemberToApi(auth: BasicAuthentication, apiId: string, body: ApiMember) {
  cy.log(`Adding member to API with id: ${apiId}`);
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/members`,
    body,
    auth,
    failOnStatusCode: false,
  });
}

export function getApiMembers(auth: BasicAuthentication, apiId: string) {
  cy.log(`Getting members for API with id: ${apiId}`);
  return cy.request({
    method: 'GET',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/members`,
    auth,
    failOnStatusCode: false,
  });
}

export function updateApi(auth: BasicAuthentication, apiId: string, apiUpdate: UpdateApiEntity, failOnStatusCode = false) {
  cy.log(`Updating API with id: ${apiId}`);
  return cy.request({
    method: 'PUT',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}`,
    auth,
    body: apiUpdate,
    failOnStatusCode,
  });
}

export function updateApiSubscription(
  auth: BasicAuthentication,
  apiId: string,
  subscriptionId: string,
  subscription: ProcessSubscriptionEntity,
) {
  cy.log(`Updating API subscription with id: ${subscriptionId}`);
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/subscriptions/${subscriptionId}/_process`,
    auth,
    body: subscription,
    failOnStatusCode: false,
  });
}

export function getApiKeys(auth: BasicAuthentication, apiId: string, subscriptionId: string) {
  cy.log(`Getting API keys for API with id: ${apiId}`);
  return cy.request({
    method: 'GET',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/subscriptions/${subscriptionId}/apikeys`,
    auth,
    failOnStatusCode: false,
  });
}

export function getApiAnalytics(auth: BasicAuthentication, apiId: string, field = 'mapped-path') {
  cy.log(`Getting API analytics for API with id: ${apiId}`);
  return requestGateway(
    {
      url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/analytics`,
      auth,
      headers: {
        'Cache-Control': 'no-cache, no-store',
        Connection: 'close',
      },
      qs: {
        type: 'group_by',
        field,
        interval: 10000,
        from: Date.now() - 5 * 60 * 1000,
        to: Date.now(),
      },
    },
    {
      validWhen: (response) => !Cypress._.isEmpty(response.body.values),
    },
  );
}
