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
import { ADMIN_USER } from '@fakers/users/users';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { Visibility } from '@gravitee/management-webclient-sdk/src/lib/models';
import faker from '@faker-js/faker';
import apiDetails from '@pageobjects/Apis/ApiDetails';
import { ApiImport } from '@model/api-imports';

describe('API Info Page functionality', () => {
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
  const apiName = faker.commerce.productName();
  const newName = apiName.replace(/ /g, '-');
  const apiVersion = faker.datatype.number();
  const apiDescription = faker.commerce.productDescription();
  const path = require('path');
  let api: ApiImport;
  let apiId: ApiImport;

  it('Verify Info Page Elements', () => {
    cy.getByDataTestId('api_list_edit_button', { timeout: 5000 }).first().click();
    cy.contains('Policy Studio').should('be.visible');
    cy.contains('Info').should('be.visible');
    cy.contains('Plans').should('be.visible');
    ApiDetails.info().click();
    cy.getByDataTestId('api_info_namefield').scrollIntoView().should('be.visible');
    cy.getByDataTestId('api_info_versionfield').should('be.visible');
    cy.getByDataTestId('api_info_descriptionfield').should('be.visible');
    cy.getByDataTestId('api_info_labelsfield').should('be.visible');
    cy.getByDataTestId('api_info_categoriesdropdown').should('be.visible').click();
    cy.getByDataTestId('api_info_categorieslist-unavailable').should('be.visible');
  });

  it('Edit Info Page and Verify changes', () => {
    cy.getByDataTestId('api_list_edit_button').first().click();
    ApiDetails.info().click();
    cy.getByDataTestId('api_info_namefield').scrollIntoView().clear().type(`${apiName}`);
    cy.getByDataTestId('api_info_versionfield').clear().type(`${apiVersion}`);
    cy.getByDataTestId('api_info_descriptionfield').clear().type(`${apiDescription}`);
    cy.getByDataTestId('api_info_savebar', { timeout: 5000 }).contains('Save').click();
    cy.contains('Configuration successfully saved!').should('be.visible');
    cy.visit('/#!/environments/default/apis/');
    cy.contains(`${apiName}`).should('be.visible');
  });

  it('Export API and verify json download', () => {
    cy.getByDataTestId('api_list_edit_button').first().click();
    cy.getByDataTestId('api_info_export_menu').click();
    cy.getByDataTestId('api_info_export_api').click();
    const downloadsFolder = Cypress.config('downloadsFolder');
    cy.readFile(path.join(downloadsFolder, `${newName}-${apiVersion}.json`)).should('exist');
  });

  it('Duplicate API and verify duplicate', () => {
    cy.getByDataTestId('api_list_edit_button').first().click();
    cy.getByDataTestId('api_info_duplicate_menu').click();
    cy.getByDataTestId('api_info_duplicate_path').type(`${newName}-duplicate`);
    cy.getByDataTestId('api_info_duplicate_version').type(`${apiVersion}`);
    cy.intercept('POST', '**/duplicate').as('duplicateApi');
    cy.getByDataTestId('api_info_duplicate_api').click();
    cy.wait('@duplicateApi').then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
      expect(interception.response.body).to.have.property('id');
      expect(interception.response.body).to.have.property('name', apiName);
      expect(interception.response.body).to.have.property('version', `${apiVersion}`);
      apiId = interception.response.body;
      cy.contains('API duplicated successfully');
    });
    cy.contains('API duplicated successfully').should('be.visible');
    cy.visit('/#!/environments/default/apis/');
    cy.contains(`${newName}-duplicate`).should('be.visible');
  });

  it('Verify Promote pop-up on info page', () => {
    cy.getByDataTestId('api_list_edit_button').first().click();
    cy.getByDataTestId('api_info_promote').click();
    cy.contains('Meet Cockpit').should('be.visible');
    cy.getByDataTestId('api_info_promote_ok').click();
  });

  // Danger Zone buttons to be continued at a later stage. See JIRA (2075)

  after(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.log('Clean up APIs');
    cy.teardownApi(api);
    cy.teardownApi(apiId);
  });
});
