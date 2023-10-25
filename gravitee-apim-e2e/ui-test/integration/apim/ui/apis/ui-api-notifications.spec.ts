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
import { Visibility } from '@gravitee/management-webclient-sdk/src/lib/models';
import { ApiImport } from '@model/api-imports';
import apiDetails from 'ui-test/support/PageObjects/Apis/ApiDetails';

let api: ApiImport;

describe('Notifications page', () => {
  beforeEach(() => {
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
    cy.visit(`/#!/environments/default/apis/${api.id}/notification-settings`);
    cy.contains('Portal Notifications').should('be.visible');
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

  it('Verify notification page elements', () => {
    cy.getByDataTestId('api_notifications_add_notification').should('be.visible');
    cy.contains('Portal Notifications').should('be.visible');
    cy.contains('Default Mail Notifications').should('be.visible');
  });

  it('Create generic notification', () => {
    cy.contains('Default Mail Notifications').should('be.visible');
    cy.getByDataTestId('api_notifications_add_notification').click();
    cy.contains('New notification').should('be.visible');
    cy.getByDataTestId('api_notifications_create_namefield').type('Test notification');
    cy.get('[type="submit"]').contains('Create').click();
    cy.contains('Notification created successfully').should('be.visible');
    cy.contains('Test notification').should('be.visible');
  });

  it('Delete generic notification', () => {
    cy.getByDataTestId('api_notifications_delete_notification').first().click();
    cy.contains('Delete notification').should('be.visible');
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('has been deleted').should('be.visible');
  });

  it('Edit notification and save changes', () => {
    cy.getByDataTestId('notifications_List_Table').first().click();
    cy.getByDataTestId('notification-detail-checkbox-APIKEY_EXPIRED').click();
    cy.contains('have unsaved changes').should('be.visible');
    cy.getByDataTestId('api_notifications_savebar', { timeout: 5000 }).contains('Save').click();
    cy.contains('Notification settings successfully saved!').should('be.visible');
  });

  it('Edit notification and discard changes', () => {
    cy.getByDataTestId('notifications_List_Table').first().click();
    cy.getByDataTestId('notification-detail-checkbox-APIKEY_EXPIRED').click();
    cy.contains('have unsaved changes').should('be.visible');
    cy.getByDataTestId('api_notifications_savebar', { timeout: 5000 }).contains('Discard').click();
    cy.contains('You have unsaved changes').should('not.be.visible');
  });

  after(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.log('Clean up APIs');
    cy.teardownApi(api);
  });
});
