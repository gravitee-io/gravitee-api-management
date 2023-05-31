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
import { ApiEntity, PlanStatus } from '../../../../../lib/management-webclient-sdk/src/lib/models';
import faker from '@faker-js/faker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('API metadata screen', () => {
  let api: ApiEntity;

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
    cy.visit(`/#!/environments/${envId}/apis/${api.id}/metadata`);
    cy.get('h2', { timeout: 40000 }).contains('API metadata').should('be.visible');
  });

  after(function () {
    cy.clearCookie('Auth-Graviteeio-APIM');
    // delete (close) Plan
    cy.request({
      method: 'POST',
      url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${api.id}/plans/${api.plans[0].id}/_close`,
      auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
    });

    // delete API
    cy.request({
      method: 'DELETE',
      url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/${api.id}`,
      auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
    });
  });

  describe('Verifying page elements', () => {
    it('check if the API metadata screen is displayed', function () {
      cy.url().should('include', 'metadata');
      cy.get('h2').contains('API metadata').should('be.visible');
      cy.getByDataTestId('api_add_metadata_button').should('be.visible').and('be.enabled');
      cy.getByDataTestId('api_metadata_key').should('be.visible');
      cy.getByDataTestId('api_metadata_name').should('be.visible');
      cy.getByDataTestId('api_metadata_format').should('be.visible');
      cy.getByDataTestId('api_metadata_value').should('be.visible');
      cy.getByDataTestId('api_metadata_defaultValue').should('be.visible');
      cy.getByDataTestId('api_metadata_actions').should('be.visible');
    });
  });

  describe('Adding API metadata', () => {
    it('should add string metadata', function () {
      cy.getByDataTestId('api_add_metadata_button').click();
      cy.getByDataTestId('api_metadata_dialog_name').click().type('string metadata key');
      cy.getByDataTestId('api_metadata_dialog_format').click();

      cy.get('mat-option').contains('string').click();
      cy.getByDataTestId('api_metadata_dialog_default_value').click().type('string metadata value');

      cy.getByDataTestId('api_metadata_dialog_save').click();
      cy.get('tbody').contains('string-metadata-key');
      cy.get('tbody').contains('string metadata value');
    });

    it('should add numeric metadata', function () {
      cy.getByDataTestId('api_add_metadata_button').click();
      cy.getByDataTestId('api_metadata_dialog_name').click().type('numeric metadata key');
      cy.getByDataTestId('api_metadata_dialog_format').click();

      cy.get('mat-option').contains('numeric').click();
      cy.getByDataTestId('api_metadata_dialog_numeric_value').click().type('123.45{enter}');

      cy.get('tbody').contains('numeric-metadata-key');
      cy.get('tbody').contains('123.45');
    });

    it('should add boolean metadata', function () {
      cy.getByDataTestId('api_add_metadata_button').click();
      cy.getByDataTestId('api_metadata_dialog_name').click().type('boolean metadata key');
      cy.getByDataTestId('api_metadata_dialog_format').click();

      cy.get('mat-option').contains('boolean').click();
      cy.getByDataTestId('api_metadata_dialog_boolean').click();
      cy.get('mat-option').contains('true').click();

      cy.getByDataTestId('api_metadata_dialog_save').click();
      cy.get('tbody').contains('boolean-metadata-key');
      cy.get('tbody').contains('true');
    });

    it('should add date metadata', function () {
      cy.getByDataTestId('api_add_metadata_button').click();
      cy.getByDataTestId('api_metadata_dialog_name').click().type('date metadata key');
      cy.getByDataTestId('api_metadata_dialog_format').click();

      cy.get('mat-option').contains('date').click();
      cy.getByDataTestId('api_metadata_dialog_date_value').click().type('12/31/2023');

      cy.getByDataTestId('api_metadata_dialog_save').click();
      cy.get('tbody').contains('date-metadata-key');
      cy.get('tbody').contains('2023-12-31');
    });
  });

  describe('Modifying API metadata', () => {
    it('should add url metadata', function () {
      cy.getByDataTestId('api_add_metadata_button').click();
      cy.getByDataTestId('api_metadata_dialog_name').click().type('url metadata key');
      cy.getByDataTestId('api_metadata_dialog_format').click();

      cy.get('mat-option').contains('url').click();
      cy.getByDataTestId('api_metadata_dialog_url_value').click().type('http://gravitee.io');

      cy.getByDataTestId('api_metadata_dialog_save').click();
      cy.get('tbody').contains('url-metadata-key');
      cy.get('tbody').contains('http://gravitee.io');
    });

    it('should modify metadata', function () {
      cy.contains('tr', 'url-metadata-key').within(() => {
        cy.getByDataTestId('api_metadata_edit_button').click();
      });
      cy.getByDataTestId('api_metadata_dialog_key').should('not.be.enabled');
      cy.getByDataTestId('api_metadata_dialog_format').should('not.be.enabled');
      cy.getByDataTestId('api_metadata_dialog_name').click().type('{moveToStart}updated ');
      cy.getByDataTestId('api_metadata_dialog_url_value').click().type('/updated');

      cy.getByDataTestId('api_metadata_dialog_save').click();
      cy.get('tbody').contains('url-metadata-key');
      cy.get('tbody').contains('updated url metadata key');
      cy.get('tbody').contains('http://gravitee.io/updated');
    });
  });

  describe('Deleting API metadata', () => {
    it('should add mail metadata', function () {
      cy.getByDataTestId('api_add_metadata_button').click();
      cy.getByDataTestId('api_metadata_dialog_name').click().type('mail metadata key');
      cy.getByDataTestId('api_metadata_dialog_format').click();

      cy.get('mat-option').contains('mail').click();
      cy.getByDataTestId('api_metadata_dialog_mail_value').click().type('john@doe.com');

      cy.getByDataTestId('api_metadata_dialog_save').click();
      cy.get('tbody').contains('mail-metadata-key');
      cy.get('tbody').contains('john@doe.com');
    });

    it('should delete metadata', function () {
      cy.contains('tr', 'mail-metadata-key') // find the row that contains 'string-metadata-key'
        .within(() => {
          cy.getByDataTestId('api_metadata_delete_button').click();
        });
      cy.getByDataTestId('confirm-dialog').click();
      cy.get('tbody').contains('mail-metadata-key').should('not.exist');
    });
  });

  describe('Sorting API metadata in overview table', () => {
    function sortTableByColumnHeader(item: string, sortOrder: 'asc' | 'desc') {
      const columnHeader = `api_metadata_${item}`;
      cy.getByDataTestId(columnHeader).click();
      let cellValues: string[] = [];
      cy.getByDataTestId(`${columnHeader}_cell`).each(($el, index, $list) => {
        cy.wrap($el)
          .invoke('text')
          .then((text) => {
            cellValues.push(text);
          });
      });
      cy.wrap(cellValues).then((initialValues) => {
        const sortedValues: string[] = sortOrder === 'asc' ? [...initialValues].sort() : [...initialValues].sort().reverse();
        expect(initialValues).to.deep.equal(sortedValues);
      });
    }

    ['key', 'name', 'format', 'value', 'defaultValue'].forEach((columnHeader: string) => {
      it(`should sort by '${columnHeader}' in ascending & descending order`, () => {
        sortTableByColumnHeader(columnHeader, 'asc');
        sortTableByColumnHeader(columnHeader, 'desc');
      });
    });
  });

  describe('Global metadata inside API documentation', () => {
    const globalMetadataName: string = `${faker.random.word()}-${faker.random.word()}`;
    const globalMetadataValue: string = `${faker.random.word()}`;

    before(() => {
      cy.request({
        method: 'POST',
        url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/configuration/metadata`,
        auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
        body: {
          format: 'STRING',
          hidden: false,
          name: globalMetadataName,
          value: globalMetadataValue,
        },
      }).ok();
    });

    it('should display global metadata in API metadata overview', function () {
      cy.contains('tr', globalMetadataName).within((globalMetadataRow) => {
        cy.getByDataTestId('api_metadata_globalIcon').should('be.visible');
        expect(globalMetadataRow).to.contain(globalMetadataName);
        expect(globalMetadataRow).to.contain(globalMetadataValue);
        cy.wrap(globalMetadataRow).find('[matTooltip="Inherited global metadata"]').should('exist');
        cy.getByDataTestId('api_metadata_edit_button').should('be.visible');
        cy.getByDataTestId('api_metadata_delete_button').should('not.exist');
      });
    });

    it('should modify global metadata in API metadata details', function () {
      cy.contains('tr', globalMetadataName).within(() => {
        cy.getByDataTestId('api_metadata_edit_button').click();
      });
      cy.getByDataTestId('api_metadata_dialog_default_value').click().type('new value for global metadata');
      cy.getByDataTestId('api_metadata_dialog_save').click();
      cy.get('tbody').contains(globalMetadataName);
      cy.get('tbody').contains('new value for global metadata');
    });

    it('should have delete button after global metadata has been modified', function () {
      cy.contains('tr', globalMetadataName).within(() => {
        cy.getByDataTestId('api_metadata_delete_button').should('be.visible');
      });
    });

    after(() => {
      cy.clearCookie('Auth-Graviteeio-APIM');
      const key = globalMetadataName.toLowerCase();
      cy.request({
        method: 'DELETE',
        url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/configuration/metadata/${key}`,
        auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
      });
    });
  });
});
