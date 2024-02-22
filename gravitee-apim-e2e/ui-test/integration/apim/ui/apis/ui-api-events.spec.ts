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

import { ADMIN_USER, API_PUBLISHER_USER } from '@fakers/users/users';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { PlanStatus } from '../../../../../lib/management-webclient-sdk/src/lib/models';
import { ApiImport } from '@model/api-imports';
import { stopApi, deployApi, startApi } from '@commands/management/api-management-commands';

const envId = 'DEFAULT';

describe('API events screen', () => {
  let api: ApiImport;

  before(() => {
    cy.request({
      method: 'POST',
      url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/import`,
      auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
      body: ApisFaker.apiImport({
        plans: [PlansFaker.plan({ status: PlanStatus.PUBLISHED })],
      }),
    }).then((response) => {
      expect(response.status).to.eq(200);
      api = response.body;
    });
  });

  beforeEach(function () {
    cy.loginInAPIM(API_PUBLISHER_USER.username, API_PUBLISHER_USER.password);
    cy.visit(`/#!/${envId}/apis/${api.id}/events`);
    cy.get('h2', { timeout: 40000 }).contains('API Events').should('be.visible');
  });

  after(function () {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.teardownApi(api);
  });

  describe('Verifying page elements', () => {
    it('check if the API Events screen is displayed correctly', function () {
      cy.url().should('include', 'events');
      cy.get('table').should('be.visible');
      cy.contains('th', 'Type').should('be.visible');
      cy.contains('th', 'Created at').should('be.visible');
      cy.contains('th', 'User').should('be.visible');
      cy.contains('td', 'There is no event (yet).').should('be.visible');
    });
  });

  describe('Verify that triggered API actions appear in API Events', () => {
    before(() => {
      deployApi(ADMIN_USER, api.id);
      startApi(ADMIN_USER, api.id);
      stopApi(API_PUBLISHER_USER, api.id);
    });

    it('should show an entry for API deployment', () => {
      cy.get('tr').should('have.length', 4);
      cy.contains('tr', 'Deployed').should('be.visible').and('contain', ADMIN_USER.username);
      cy.contains('tr', 'Started').should('be.visible').and('contain', ADMIN_USER.username);
      cy.contains('tr', 'Stopped').should('be.visible').and('contain', API_PUBLISHER_USER.username);
    });
  });
});
