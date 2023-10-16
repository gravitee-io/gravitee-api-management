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
import faker from '@faker-js/faker';
import { Visibility } from '@gravitee/management-webclient-sdk/src/lib/models';
import { ApiImport } from '@model/api-imports';

import apiDetails from 'ui-test/support/PageObjects/Apis/ApiDetails';

describe('API Plans Feature', () => {
  beforeEach(() => {
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
    cy.visit('/#!/environments/default/apis/');
    cy.url().should('include', '/apis/');
  });

  before(() => {
    cy.log('Import (create) v2 API');
    cy.request({
      method: 'POST',
      url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/import`,
      auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
      body: ApisFaker.apiImport({ visibility: Visibility.PUBLIC }),
    }).then((response) => {
      expect(response.status).to.eq(200);
      api = response.body;
    });
  });

  const ApiDetails = new apiDetails();
  let api: ApiImport;


  it('Verify Notifcation page elements', () => {
    cy.getByDataTestId('api_list_edit_button').first().click();
    ApiDetails.notificationMenuItem().click();
    cy.getByDataTestId('api_notifications_add_notification').should('be.visible');
  });

  it('Create generic notification', () => {
    cy.getByDataTestId('api_list_edit_button').first().click();
    ApiDetails.notificationMenuItem().click();
    cy.contains('Portal Notifications').should('be.visible')
    cy.contains('Default Mail Notifications').should('be.visible')
    cy.getByDataTestId('api_notifications_add_notification').click();
    cy.contains('New notification').should('be.visible')
    cy.getByDataTestId('api_notifications_create_namefield').type('Test')
    cy.get('[type="submit"]').contains('Create').click();
    cy.contains('Notification created successfully').should('be.visible')
  });

  it('Delete generic notification', () => {
    cy.getByDataTestId('api_list_edit_button').first().click();
    ApiDetails.notificationMenuItem().click();
    cy.getByDataTestId('api_notifications_delete_notification').first().click();
    cy.contains('Delete notification').should('be.visible')
    cy.getByDataTestId('confirm-dialog').click()
    cy.contains('has been deleted').should('be.visible')
  });

  after(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.log('Clean up APIs');
    cy.teardownApi(api);
  });
});
