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
import { ApiImport } from '@model/api-imports';
import apiDetails from 'ui-test/support/PageObjects/Apis/ApiDetails';
import { PlanStatus } from '../../../../../lib/management-webclient-sdk/src/lib/models';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';

let api: ApiImport;

describe('Notifications page', () => {
  before(() => {
    cy.log('Import (create) v2 API');
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

  beforeEach(() => {
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
    cy.visit(`/#!/default/apis/${api.id}/notification-settings`);
    cy.getByDataTestId('notifications_title').should('be.visible').contains('Notification settings');
    cy.getByDataTestId('notifications_list_table').should('be.visible').contains('Portal Notification');
  });

  it('Verify notification page elements', () => {
    cy.getByDataTestId('api_notifications_add_notification').should('be.visible').contains('Add notification');
    cy.getByDataTestId('notifications_title').should('be.visible').contains('Notification settings');
  });

  it('Create generic notification', () => {
    cy.contains('Default Mail Notifications').should('be.visible');
    cy.getByDataTestId('api_notifications_add_notification').click();
    cy.contains('New notification', { timeout: 2500 }).should('be.visible');
    cy.getByDataTestId('api_notifications_create_namefield').type('Test notification');
    cy.getByDataTestId('create_notification_button').should('be.visible').contains('Create').click();
    cy.contains('Notification created successfully').should('be.visible');
    cy.contains('Test notification').should('be.visible');
  });

  it('Delete generic notification', () => {
    cy.getByDataTestId('api_notifications_delete_notification').first().click();
    cy.contains('Delete notification').should('be.visible');
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('has been deleted').should('be.visible');
  });

  describe('Edit notification', () => {
    beforeEach(() => {
      cy.getByDataTestId('notifications_list_table').first().click();
      cy.getByDataTestId('notification_detail_checkbox_API_DEPLOYED').click();
      cy.contains('have unsaved changes').should('be.visible');
    });

    it('Edit notification, save changes and check notifications work', () => {
      cy.getByDataTestId('api_notification_savebar').contains('Save').click();
      cy.contains('Notification settings successfully saved!').should('be.visible');
      cy.getByDataTestId('deploy_banner').contains('Deploy API').click();
      cy.getByDataTestId('confirm_api_deploy').contains('Deploy').click();
      cy.contains('API successfully deployed.').should('be.visible');
      cy.getByDataTestId('notification_badge').click();
      retryNotifications(4); //4x2.5s to equal the /notifications scheduled request time
      cy.getByDataTestId('notification_menu').should('be.visible').contains('Delete all').click();
    });

    it('Edit notification and discard changes', () => {
      cy.getByDataTestId('api_notification_savebar').contains('Discard').click();
      cy.getByDataTestId('notification_detail_checkbox_API_DEPLOYED').should('not.be.checked');
      cy.contains('You have unsaved changes').should('not.be.visible');
    });
  });

  after(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.log('Clean up APIs');
    cy.teardownApi(api);
  });

  function retryNotifications(retryTimes: number = 0) {
    if (retryTimes <= 0) {
      throw new Error('Unable to see notification after retries');
    }
    try {
      cy.getByDataTestId('notification_menu', { timeout: 2500 }).should('be.visible').contains('API deployed');
    } catch (e) {
      retryNotifications(retryTimes - 1);
    }
  }
});
