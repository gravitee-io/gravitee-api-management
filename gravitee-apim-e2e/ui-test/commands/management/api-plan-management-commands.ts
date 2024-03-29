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
import { NewPlanEntity } from '@model/plan';
import { BasicAuthentication } from '@model/users';

export function createPlan(auth: BasicAuthentication, apiId: string, body: Partial<NewPlanEntity>, failOnStatusCode = false) {
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/plans`,
    auth,
    body,
    failOnStatusCode,
  });
}

export function deletePlan(auth: BasicAuthentication, apiId: string, planId: string, failOnStatusCode = false) {
  return cy.request({
    method: 'DELETE',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/plans/${planId}`,
    auth,
    failOnStatusCode,
  });
}

export function closePlan(auth: BasicAuthentication, apiId: string, planId: string) {
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/plans/${planId}/_close`,
    auth,
    failOnStatusCode: false,
  });
}

export function closeV4Plan(auth: BasicAuthentication, apiId: string, planId: string, envId = 'DEFAULT') {
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}/management/v2/environments/${envId}/apis/${apiId}/plans/${planId}/_close`,
    auth,
    failOnStatusCode: false,
  });
}

export function listV4Plans(auth: BasicAuthentication, apiId: string, envId = 'DEFAULT') {
  return cy.request({
    method: 'GET',
    url: `${Cypress.env('managementApi')}/management/v2/environments/${envId}/apis/${apiId}/plans`,
    auth,
    failOnStatusCode: false,
  });
}

export function publishPlan(auth: BasicAuthentication, apiId: string, planId: string, failOnStatusCode = false) {
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${apiId}/plans/${planId}/_publish`,
    auth,
    failOnStatusCode,
  });
}
