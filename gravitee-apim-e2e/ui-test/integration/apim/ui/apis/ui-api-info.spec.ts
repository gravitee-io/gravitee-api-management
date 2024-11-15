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
import faker from '@faker-js/faker';
import ApiDetails from 'ui-test/support/PageObjects/Apis/ApiDetails';
import { Api } from '@model/apis';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { HttpListener, MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { ApiType, ApiV4, FlowV4 } from '@gravitee/management-v2-webclient-sdk/src/lib';

const envId = 'DEFAULT';

describe('API Info Page functionality', () => {
  let api: Api;
  let duplicateApi: ApiV4;
  let v4dangerzoneApi: ApiV4;
  let v4infoApi: ApiV4;
  const apiDetails = new ApiDetails();
  const apiName = faker.commerce.productName();
  const apiVersion = faker.datatype.number();
  const apiDescription = faker.lorem.words(10);
  const apiFileName = `${apiName.replace(/ /g, '-')}-${apiVersion}`;
  const path = require('path');
  const httpListener1: HttpListener = MAPIV2ApisFaker.newHttpListener();
  const httpListener2: HttpListener = MAPIV2ApisFaker.newHttpListener();
  const commonFlow: FlowV4 = MAPIV2ApisFaker.newFlow();

  beforeEach(() => {
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
    cy.visit('/#!/default/apis/');
    cy.url().should('include', '/apis/');
  });

  before(() => {
    cy.log('Import (create) v2 API for Info');
    cy.request({
      method: 'POST',
      url: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}/apis/import`,
      auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
      body: ApisFaker.apiImport({ visibility: Visibility.PUBLIC }),
    }).then((response) => {
      expect(response.status).to.eq(200);
      api = { ...response.body };
    });

    cy.log('Create v4 API for Info');
    cy.request({
      method: 'POST',
      url: `${Cypress.env('managementApi')}/management/v2/environments/DEFAULT/apis`,
      auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
      body: MAPIV2ApisFaker.newApi({
        type: ApiType.PROXY,
        listeners: [httpListener1],
        endpointGroups: [MAPIV2ApisFaker.newHttpEndpointGroup()],
      }),
    }).then((response) => {
      expect(response.status).to.eq(201);
      v4infoApi = response.body;
    });

    cy.log('Create v4 API for Dangerzone');
    cy.request({
      method: 'POST',
      url: `${Cypress.env('managementApi')}/management/v2/environments/DEFAULT/apis`,
      auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
      body: MAPIV2ApisFaker.newApi({
        type: ApiType.PROXY,
        listeners: [httpListener2],
        endpointGroups: [MAPIV2ApisFaker.newHttpEndpointGroup()],
        flows: [commonFlow],
      }),
    }).then((response) => {
      expect(response.status).to.eq(201);
      v4dangerzoneApi = response.body;

      cy.log('Create a plan with a flow');
      cy.request({
        method: 'POST',
        url: `${Cypress.env('managementApi')}/management/v2/environments/${envId}/apis/${v4dangerzoneApi.id}/plans`,
        auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
        body: MAPIV2PlansFaker.newPlanV4(),
      })
        .then((response) => {
          expect(response.status).to.eq(201);
          return response.body.id;
        })
        .then((planId) => {
          cy.log('Publish Plan');
          cy.request({
            method: 'POST',
            url: `${Cypress.env('managementApi')}/management/v2/environments/DEFAULT/apis/${v4dangerzoneApi.id}/plans/${planId}/_publish`,
            auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
          }).then((response) => {
            expect(response.status).to.eq(200);
          });
        });
    });
  });

  it('Verify Info Page Elements', () => {
    cy.visit(`/#!/DEFAULT/apis/${v4infoApi.id}`);
    apiDetails.infoMenuItem().click();
    apiDetails.policyStudioMenuItem().should('be.visible');
    apiDetails.infoMenuItem().should('be.visible');
    apiDetails.consumersMenuItem().should('be.visible');
    apiDetails.infoMenuItem().click();
    cy.getByDataTestId('api_info_namefield').scrollIntoView().should('be.visible');
    cy.getByDataTestId('api_info_versionfield').should('be.visible');
    cy.getByDataTestId('api_info_descriptionfield').should('be.visible');
    cy.getByDataTestId('api_info_labelsfield').should('be.visible');
    cy.getByDataTestId('api_info_categoriesdropdown').should('be.visible').click();
    cy.getByDataTestId('api_info_categorieslist-unavailable').should('be.visible');
  });

  it('Edit Info Page and Verify changes', () => {
    cy.visit(`/#!/DEFAULT/apis/${v4infoApi.id}`);
    cy.getByDataTestId('api_info_namefield').scrollIntoView().clear().type(`${apiName}`);
    cy.getByDataTestId('api_info_versionfield').clear().type(`${apiVersion}`);
    cy.getByDataTestId('api_info_descriptionfield').clear().type(`${apiDescription}`);
    cy.getByDataTestId('api_info_savebar', { timeout: 5000 }).contains('Save').click();
    cy.contains('Configuration successfully saved!').should('be.visible');
    cy.getByDataTestId('api_info_namefield').scrollIntoView().should('have.value', apiName);
    cy.getByDataTestId('api_info_versionfield').should('have.value', apiVersion);
    cy.getByDataTestId('api_info_descriptionfield').should('have.value', apiDescription);
    cy.visit('/#!/default/apis/');
    cy.contains(apiName, { timeout: 60000 }).should('be.visible');
  });

  it('Export API and verify json download (v4)', () => {
    cy.visit(`/#!/DEFAULT/apis/${v4infoApi.id}`);
    cy.getByDataTestId('api_info_export_menu').click();
    cy.getByDataTestId('api_info_export_api').click();

    const downloadsFolder = Cypress.config('downloadsFolder');
    cy.readFile(path.join(downloadsFolder, `${apiFileName}.json`)).should((file) => {
      expect(file.api.name).to.equal(apiName);
      expect(file.api.description).to.equal(apiDescription);
    });
  });

  it('Duplicate API and verify duplicate', () => {
    cy.visit(`/#!/DEFAULT/apis/${v4infoApi.id}`);
    cy.getByDataTestId('api_info_duplicate_menu', { timeout: 60000 }).click();
    cy.getByDataTestId('api_info_duplicate_path').type(`/${apiFileName}-duplicate`);
    cy.getByDataTestId('api_info_duplicate_version').type(`${apiVersion}`);
    cy.intercept('POST', '**/_duplicate').as('duplicateApi');
    cy.getByDataTestId('api_info_duplicate_api').click();
    cy.wait('@duplicateApi').then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
      expect(interception.response.body).to.have.property('id');
      expect(interception.response.body).to.have.property('name', `${apiName}`);
      expect(interception.response.body).to.have.property('apiVersion', `${apiVersion}`);
      duplicateApi = interception.response.body;
    });
    cy.contains('API duplicated successfully').should('be.visible');
    cy.visit('/#!/default/apis/');
    cy.contains(`${apiFileName}-duplicate`).should('be.visible');
  });

  it('Verify Promote pop-up on info page (v2)', () => {
    cy.visit(`/#!/DEFAULT/apis/${api.id}`);
    cy.getByDataTestId('api_info_promote').click();
    cy.contains('Meet Gravitee Cloud').should('be.visible');
    cy.getByDataTestId('api_info_promote_ok').click();
  });

  // Danger Zone

  it('Danger Zone - Start and Stop the API (without and with published plan)', () => {
    cy.visit(`/#!/DEFAULT/apis/${v4infoApi.id}`);
    cy.getByDataTestId('api_info_dangerzone_start_api').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('can not be started without at least one published plan');

    cy.visit(`/#!/DEFAULT/apis/`);
    cy.getByDataTestId('search').type(`${v4dangerzoneApi.id}{enter}`);
    cy.getByDataTestId('api_list_table_row').should('have.length', 1);
    cy.getByDataTestId('api_list_table_row').should('contain.text', v4dangerzoneApi.name);
    cy.contains('tr', v4dangerzoneApi.name).find('[data-testid="api_list_edit_button"]').click();

    cy.url().should('include', v4dangerzoneApi.id);
    cy.contains(`${v4dangerzoneApi.name}`).should('be.visible');
    cy.getByDataTestId('api_info_dangerzone_start_api').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('The API has been started with success.');
    cy.getByDataTestId('api_info_dangerzone_stop_api').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('The API has been stopped with success.');
  });

  it('Danger Zone - Publish and Unpublish the API', () => {
    cy.visit(`/#!/DEFAULT/apis/${v4dangerzoneApi.id}`);
    cy.getByDataTestId('api_info_dangerzone_publish_api').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('The API has been published with success.');
    cy.getByDataTestId('api_info_dangerzone_unpublish_api').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('The API has been unpublished with success.');
  });

  it('Danger Zone - Make Public and Make Private the API', () => {
    cy.visit(`/#!/DEFAULT/apis/${v4dangerzoneApi.id}`);
    cy.getByDataTestId('api_info_dangerzone_make_public', { timeout: 60000 }).click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('The API has been made Public with success.');
    cy.getByDataTestId('api_info_dangerzone_make_private').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('The API has been made Private with success.');
  });

  it('Danger Zone - Deprecate the API', () => {
    cy.visit(`/#!/DEFAULT/apis/${v4dangerzoneApi.id}`);
    cy.getByDataTestId('api_info_dangerzone_deprecate_api').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('The API has been deprecated with success.');
    cy.contains('This API is deprecated.');
    cy.getByDataTestId('api_info_dangerzone_publish_api').should('not.exist');
    cy.getByDataTestId('api_info_dangerzone_unpublish_api').should('not.exist');
    cy.getByDataTestId('api_info_dangerzone_make_private').should('not.exist');
    cy.getByDataTestId('api_info_dangerzone_make_public').should('not.exist');
    cy.getByDataTestId('api_info_dangerzone_deprecate_api').should('not.exist');
  });

  it('Danger Zone - Delete the API', () => {
    cy.visit(`/#!/DEFAULT/apis/${v4dangerzoneApi.id}`);
    cy.getByDataTestId('api_dangerzone_delete_api').click();
    cy.getByDataTestId('confirm-input-dialog').type(`${v4dangerzoneApi.name}`);
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains('The API has been deleted.');
    cy.contains(`${v4dangerzoneApi.name}`).should('not.exist');
  });

  after(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.log('Clean up APIs');
    cy.teardownApi(api);
    cy.teardownV4Api(v4infoApi.id);
    cy.teardownV4Api(duplicateApi.id);
  });
});
