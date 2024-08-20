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
import { Api } from '@model/apis';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { Visibility } from '../../../../../lib/management-webclient-sdk/src/lib/models';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { ApiImport } from '@model/api-imports';
import { ApiV4 } from '../../../../../lib/management-v2-webclient-sdk/src/lib';

const envId = 'DEFAULT';

describe('API List feature', { defaultCommandTimeout: 10000 }, () => {
  describe('Verifying page elements', () => {
    beforeEach(() => {
      cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
      cy.visit('/#!/default/apis/');
      cy.getByDataTestId('api_list_addApi_button', { timeout: 60000 }).should('be.visible').and('contain.text', 'Add API');
    });

    it('check if the API list screen is displayed correctly', function () {
      cy.getByDataTestId('search').should('be.visible');
      cy.get('td:contains("There is no API (yet)")').should('be.visible');
      cy.getByDataTestId('paginator-header').should('be.visible');
      cy.getByDataTestId('paginator-footer').should('be.visible');
    });

    it("should load API creation page when 'Add API' button is clicked", function () {
      cy.getByDataTestId('api_list_addApi_button').click();
      cy.url().should('include', '/apis/new');
      cy.get('h1').contains('Choose API creation method').should('be.visible');
    });

    it('should have correct table header', function () {
      cy.getByDataTestId('api_list_table_header')
        .should('contain.text', 'Name')
        .and('contain.text', 'Definition')
        .and('contain.text', 'Status')
        .and('contain.text', 'Access')
        .and('contain.text', 'Tags')
        .and('contain.text', 'Owner')
        .and('contain.text', 'Visibility');
    });
  });

  describe('Verifying API list', () => {
    let api: ApiImport[];
    let v4api: ApiV4[];
    const noOfApis = 4;

    before(() => {
      api = [];
      v4api = [];

      Cypress._.times(noOfApis, () => {
        cy.log('Create v4 API');
        cy.request({
          method: 'POST',
          url: `${Cypress.env('managementApi')}/management/v2/environments/DEFAULT/apis`,
          auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
          body: MAPIV2ApisFaker.newApi({
            listeners: [
              MAPIV2ApisFaker.newSubscriptionListener({
                entrypoints: [
                  {
                    type: 'webhook',
                  },
                ],
              }),
            ],
            endpointGroups: [
              {
                name: 'default-group',
                type: 'mock',
                endpoints: [
                  {
                    name: 'default',
                    type: 'mock',
                    configuration: {
                      messageInterval: 0,
                    },
                  },
                ],
              },
            ],
          }),
        }).then((response) => {
          expect(response.status).to.eq(201);
          v4api.push(response.body);
        });

        cy.log('Import (create) v2 API');
        cy.request({
          method: 'POST',
          url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/import`,
          auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
          body: ApisFaker.apiImport({ visibility: Visibility.PUBLIC }),
        }).then((response) => {
          expect(response.status).to.eq(200);
          api.push(response.body);
        });
      });
    });

    beforeEach(() => {
      cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
      cy.visit(`/#!/default/apis/`);
      cy.getByDataTestId('api_list_addApi_button', { timeout: 60000 }).should('be.visible').and('contain.text', 'Add API');
    });

    describe('Verify element of API list filled with sample data', function () {
      it(`should display ${noOfApis} v2 APIs and ${noOfApis} v4 APIs `, function () {
        cy.get('tr:contains(V4 - Message)').should('have.length', noOfApis);
        cy.get('tr:contains(V2)').should('have.length', noOfApis);
      });

      it(`should have ${noOfApis} APIs with "api1" and ${noOfApis} with "admin" as owner`, function () {
        cy.get('tr:contains(api1)').should('have.length', noOfApis);
        cy.get('tr:contains(admin)').should('have.length', noOfApis);
      });

      it('should display lock symbol for private APIs', function () {
        cy.get('mat-icon:contains(lock)').should('have.length', noOfApis);
      });

      it('should display public symbol for public APIs', function () {
        cy.get('mat-icon:contains(public)').should('have.length', noOfApis);
      });

      it('should have edit button for all displayed APIs', function () {
        cy.getByDataTestId('api_list_edit_button').should('have.length', noOfApis * 2);
      });
    });

    describe('Verify paging', function () {
      it(`should display all ${noOfApis * 2} APIs`, function () {
        cy.getByDataTestId('api_list_table_row').should('have.length', noOfApis * 2);
      });

      it(`should switch view to 50 when 50 items per page selected`, function () {
        cy.getByDataTestId('paginator-header').within(() => {
          cy.get('mat-select').click({ force: true });
        });
        cy.get('mat-option').contains('50').first().click();
        cy.url().should('include', 'size=50');
      });
    });

    describe('Search for APIs', function () {
      it(`should display ${noOfApis} APIs when filtering by owner`, function () {
        cy.getByDataTestId('search').type('ownerName:admin{enter}');
        cy.getByDataTestId('api_list_table_row').should('contain.text', 'admin');
      });

      it(`should display ${noOfApis} APIs when searching for v2 APIs`, function () {
        cy.getByDataTestId('search').type(`definition_version: 2.0.0{enter}`);
        cy.getByDataTestId('api_list_table_row').should('have.length', noOfApis);
        cy.getByDataTestId('api_list_table_row').should('contain.text', 'admin');
      });

      it(`should display ${noOfApis} APIs when searching for v4 APIs`, function () {
        cy.getByDataTestId('search').type(`definition_version: 4.0.0{enter}`);
        cy.getByDataTestId('api_list_table_row').should('have.length', noOfApis);
        cy.getByDataTestId('api_list_table_row').should('contain.text', 'api1');
      });
    });

    after(() => {
      cy.clearCookie('Auth-Graviteeio-APIM');
      cy.log('Clean up APIs');
      Cypress._.times(noOfApis, (i) => {
        cy.request({
          method: 'DELETE',
          url: `${Cypress.env('managementApi')}/management/v2/environments/${envId}/apis/${v4api[i].id}?closePlans=true`,
          auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
        }).then((response) => {
          expect(response.status).to.eq(204);
        });
        cy.teardownApi(api[i]);
      });
    });
  });
});
