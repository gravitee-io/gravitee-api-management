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
import faker from '@faker-js/faker';
import { Visibility } from '@gravitee/management-webclient-sdk/src/lib/models';
import { ApiImport } from '@model/api-imports';

import apiDetails from 'ui-test/support/PageObjects/Apis/ApiDetails';

describe('API Plans Feature', () => {
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

  beforeEach(() => {
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
    cy.visit('/#!/default/apis/');
    cy.url().should('include', '/apis/');
    cy.contains(api.name).should('exist', { timeout: 60000 });
    cy.contains('tr', api.name).find('[data-testid="api_list_edit_button"]').click();
    ApiDetails.plansMenuItem().click();
    cy.intercept('POST', '**/_close').as('closePlan');
  });

  const ApiDetails = new apiDetails();
  let api: ApiImport;
  const planName = faker.commerce.product();
  const planDescription = faker.lorem.words(5);

  it('Verify Plan page elements', () => {
    cy.contains('STAGING').scrollIntoView().should('be.visible');
    cy.contains('PUBLISHED').should('be.visible');
    cy.contains('DEPRECATED').should('be.visible');
    cy.contains('CLOSED').should('be.visible');
    cy.contains('There is no plan (yet).').should('be.visible');
    cy.getByDataTestId('api_plans_add_plan_button').scrollIntoView().should('be.visible');
  });

  it('Create a generic New Plan (API Key), verify and delete', () => {
    // create API Key plan
    cy.getByDataTestId('api_plans_add_plan_button').click();
    cy.contains('API Key').click();
    cy.url().should('include', '/new?selectedPlanMenuItem=API_KEY');
    cy.getByDataTestId('api_plans_name_field').type(`${planName}-APIKey`);
    cy.getByDataTestId('api_plans_description_field').type(`${planDescription} API Key`);
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.contains('Propagate API Key').should('exist').scrollIntoView().should('be.visible');
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.get('gio-form-label').contains('Rate Limiting').should('exist');
    cy.contains('Quota').should('be.visible');
    cy.contains('Resource Filtering').should('be.visible');
    cy.get('[type="submit"]').contains('Create').click();

    // verify
    cy.contains('Configuration successfully saved!').should('be.visible');
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains(`${planName}-APIKey`).should('be.visible');

    // close API Key plan
    cy.contains('tr', `${planName}-APIKey`).find('[data-testid="api_plans_close_plan_button"]').click();
    cy.get(`[placeholder="${planName}-APIKey"]`).type(`${planName}-APIKey`);
    cy.getByDataTestId('confirm-dialog').click();
    cy.wait('@closePlan');
    cy.contains(`The plan ${planName}-APIKey has been closed with success.`).should('be.visible');
    cy.contains(`${planName}-APIKey`).should('not.exist');
  });

  it('Create a generic New Plan (OAuth2), verify and delete', () => {
    // create OAuth2 plan
    cy.getByDataTestId('api_plans_add_plan_button').click();
    cy.contains('OAuth2').click();
    cy.url().should('include', '/new?selectedPlanMenuItem=OAUTH2');
    cy.getByDataTestId('api_plans_name_field').type(`${planName}-OAuth2`);
    cy.getByDataTestId('api_plans_description_field').type(`${planDescription} OAuth2`);
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.get('[role="combobox"]').eq(4).type('Test');
    // ^ I can't find html element for this, this doesn't feel good but was best I could do as OAuth2 Resource a mandatory field
    cy.contains('OAuth2 resource').should('exist').scrollIntoView().should('be.visible');
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.get('gio-form-label').contains('Rate Limiting').should('exist');
    cy.contains('Quota').should('be.visible');
    cy.contains('Resource Filtering').should('be.visible');
    cy.get('[type="submit"]').contains('Create').click();

    // verify
    cy.contains('Configuration successfully saved!').should('be.visible');
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains(`${planName}-OAuth2`).should('be.visible');

    // close OAuth2 plan
    cy.contains('tr', `${planName}-OAuth2`).find('[data-testid="api_plans_close_plan_button"]').click();
    cy.get(`[placeholder="${planName}-OAuth2"]`).type(`${planName}-OAuth2`);
    cy.getByDataTestId('confirm-dialog').click();
    cy.wait('@closePlan');
    cy.contains(`The plan ${planName}-OAuth2 has been closed with success.`).should('be.visible');
    cy.contains(`${planName}-OAuth2`).should('not.exist');
  });

  it('Create a generic New Plan (JWT), verify and delete', () => {
    // create JWT plan
    cy.getByDataTestId('api_plans_add_plan_button').click();
    cy.contains('JWT').click();
    cy.url().should('include', '/new?selectedPlanMenuItem=JWT');
    cy.getByDataTestId('api_plans_name_field').type(`${planName}-JWT`);
    cy.getByDataTestId('api_plans_description_field').type(`${planDescription} JWT`);
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.contains('JWKS resolver').should('exist').scrollIntoView().should('be.visible');
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.get('gio-form-label').contains('Rate Limiting').should('exist');
    cy.contains('Quota').should('be.visible');
    cy.contains('Resource Filtering').should('be.visible');
    cy.get('[type="submit"]').contains('Create').click();

    // verify
    cy.contains('Configuration successfully saved!').should('be.visible');
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains(`${planName}-JWT`).should('be.visible');

    // close JWT plan
    cy.contains('tr', `${planName}-JWT`).find('[data-testid="api_plans_close_plan_button"]').click();
    cy.get(`[placeholder="${planName}-JWT"]`).type(`${planName}-JWT`);
    cy.getByDataTestId('confirm-dialog').click();
    cy.wait('@closePlan');
    cy.contains(`The plan ${planName}-JWT has been closed with success.`).should('be.visible');
    cy.contains(`${planName}-JWT`).should('not.exist');
  });

  it('Create a generic New Plan (Keyless), verify and delete', () => {
    // create keyless plan
    cy.getByDataTestId('api_plans_add_plan_button').click();
    cy.contains('Keyless (public)').click();
    cy.url().should('include', '/new?selectedPlanMenuItem=KEY_LESS');
    cy.getByDataTestId('api_plans_name_field').type(`${planName}-Keyless`);
    cy.getByDataTestId('api_plans_description_field').type(`${planDescription} Keyless`);
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.get('gio-form-label').contains('Rate Limiting').should('exist');
    cy.contains('Quota').should('be.visible');
    cy.contains('Resource Filtering').should('be.visible');
    cy.get('[type="submit"]').contains('Create').click();

    // verify
    cy.contains('Configuration successfully saved!').should('be.visible');
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains(`${planName}-Keyless`).should('be.visible');

    // close keyless plan
    cy.contains('tr', `${planName}-Keyless`).find('[data-testid="api_plans_close_plan_button"]').click();
    cy.get(`[placeholder="${planName}-Keyless"]`).type(`${planName}-Keyless`);
    cy.getByDataTestId('confirm-dialog').click();
    cy.wait('@closePlan', { requestTimeout: 10000 });
    cy.contains(`The plan ${planName}-Keyless has been closed with success.`).should('be.visible');
    cy.contains(`${planName}-Keyless`).should('not.exist');
  });

  it('Create a New Plan (Keyless), select Design the Plan, verify path and delete plan', () => {
    // create keyless plan
    cy.getByDataTestId('api_plans_add_plan_button').click();
    cy.contains('Keyless (public)').click();
    cy.url().should('include', '/new?selectedPlanMenuItem=KEY_LESS');
    cy.getByDataTestId('api_plans_name_field').type(`${planName}-Keyless`);
    cy.getByDataTestId('api_plans_description_field').type(`${planDescription} Keyless`);
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.get('[type="submit"]').contains('Create').click();
    cy.contains('Configuration successfully saved!').should('be.visible');

    // select Design the Plan
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains(`${planName}-Keyless`).should('be.visible');
    cy.contains('tr', `${planName}-Keyless`).find('[data-testid="api_plans_design_plan_button"]').click();
    cy.url().should('include', 'policy-studio/');

    // close keyless plan
    cy.visit('/#!/default/apis/');
    cy.contains('tr', api.name).find('[data-testid="api_list_edit_button"]').click();
    ApiDetails.plansMenuItem().click();
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains('tr', `${planName}-Keyless`).find('[data-testid="api_plans_close_plan_button"]').click();
    cy.get(`[placeholder="${planName}-Keyless"]`).type(`${planName}-Keyless`);
    cy.getByDataTestId('confirm-dialog').click();
    cy.wait('@closePlan');
    cy.contains(`The plan ${planName}-Keyless has been closed with success.`).should('be.visible');
    cy.contains(`${planName}-Keyless`).should('not.exist');
  });

  it('Create a New Plan (Keyless), edit the plan and delete plan', () => {
    // create keyless plan
    cy.getByDataTestId('api_plans_add_plan_button').click();
    cy.contains('Keyless (public)').click();
    cy.url().should('include', '/new?selectedPlanMenuItem=KEY_LESS');
    cy.getByDataTestId('api_plans_name_field').type(`${planName}-Keyless`);
    cy.getByDataTestId('api_plans_description_field').type(`${planDescription} Keyless`);
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.get('[type="submit"]').contains('Create').click();
    cy.contains('Configuration successfully saved!').should('be.visible');

    // edit plan
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains(`${planName}-Keyless`).should('be.visible');
    cy.contains('tr', `${planName}-Keyless`).find('[data-testid="api_plans_edit_plan_button"]').click();
    cy.getByDataTestId('api_plans_name_field').type('EDIT');
    cy.contains('Configuration successfully saved!').should('not.exist');
    cy.get('[type="submit"]').contains('Save').click();
    cy.contains('Configuration successfully saved!').should('be.visible');
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains(`${planName}-KeylessEDIT`).should('be.visible');

    // close keyless plan
    cy.contains('tr', `${planName}-KeylessEDIT`).find('[data-testid="api_plans_close_plan_button"]').click();
    cy.get(`[placeholder="${planName}-KeylessEDIT"]`).type(`${planName}-KeylessEDIT`);
    cy.getByDataTestId('confirm-dialog').click();
    cy.wait('@closePlan');
    cy.contains(`The plan ${planName}-KeylessEDIT has been closed with success.`).should('be.visible');
    cy.contains(`${planName}-KeylessEDIT`).should('not.exist');
  });

  it('Create a New Plan (Keyless), publish the plan and delete plan', () => {
    // create keyless plan
    cy.getByDataTestId('api_plans_add_plan_button').click();
    cy.contains('Keyless (public)').click();
    cy.url().should('include', '/new?selectedPlanMenuItem=KEY_LESS');
    cy.getByDataTestId('api_plans_name_field').type(`${planName}-Keyless`);
    cy.getByDataTestId('api_plans_description_field').type(`${planDescription} Keyless`);
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.get('[type="submit"]').contains('Create').click();
    cy.contains('Configuration successfully saved!').should('be.visible');

    // publish plan
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains(`${planName}-Keyless`).should('be.visible');
    cy.contains('tr', `${planName}-Keyless`).find('[data-testid="api_plans_publish_plan_button"]').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains(`The plan ${planName}-Keyless has been published with success.`).should('be.visible');
    cy.contains(`${planName}-Keyless`).should('not.exist');
    cy.get('[type="button"]').contains('PUBLISHED').click();
    cy.url().should('include', '/plans?status=PUBLISHED');
    cy.contains(`${planName}-Keyless`).should('be.visible');

    // close keyless plan
    cy.contains('tr', `${planName}-Keyless`).find('[data-testid="api_plans_close_plan_button"]').click();
    cy.get(`[placeholder="${planName}-Keyless"]`).type(`${planName}-Keyless`);
    cy.getByDataTestId('confirm-dialog').click();
    cy.wait('@closePlan');
    cy.contains(`The plan ${planName}-Keyless has been closed with success.`).should('be.visible');
    cy.contains(`${planName}-Keyless`).should('not.exist');
  });

  it('Create a New Plan (Keyless), publish then deprecate the plan and delete plan', () => {
    // create keyless plan
    cy.getByDataTestId('api_plans_add_plan_button').click();
    cy.contains('Keyless (public)').click();
    cy.url().should('include', '/new?selectedPlanMenuItem=KEY_LESS');
    cy.getByDataTestId('api_plans_name_field').type(`${planName}-Keyless`);
    cy.getByDataTestId('api_plans_description_field').type(`${planDescription} Keyless`);
    cy.getByDataTestId('api_plans_nextstep').click();
    cy.get('[type="submit"]').contains('Create').click();
    cy.contains('Configuration successfully saved!').should('be.visible');

    // publish plan
    cy.get('[type="button"]').contains('STAGING').click();
    cy.url().should('include', '/plans?status=STAGING');
    cy.contains(`${planName}-Keyless`).should('be.visible');

    cy.contains('tr', `${planName}-Keyless`).find('[data-testid="api_plans_publish_plan_button"]').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains(`The plan ${planName}-Keyless has been published with success.`).should('be.visible');
    cy.contains(`${planName}-Keyless`).should('not.exist');
    cy.get('[type="button"]').contains('PUBLISHED').click();
    cy.url().should('include', '/plans?status=PUBLISHED');
    cy.contains(`${planName}-Keyless`).should('be.visible');

    // deprecate plan
    cy.contains('tr', `${planName}-Keyless`).find('[data-testid="api_plans_deprecate_plan_button"]').click();
    cy.getByDataTestId('confirm-dialog').click();
    cy.contains(`The plan ${planName}-Keyless has been deprecated with success.`).should('be.visible');
    cy.get('[type="button"]').contains('DEPRECATED').click();
    cy.url().should('include', '/plans?status=DEPRECATED');
    cy.contains(`${planName}-Keyless`).should('be.visible');

    // close keyless plan
    cy.contains('tr', `${planName}-Keyless`).find('[data-testid="api_plans_close_plan_button"]').click();
    cy.get(`[placeholder="${planName}-Keyless"]`).type(`${planName}-Keyless`);
    cy.getByDataTestId('confirm-dialog').click();
    cy.wait('@closePlan');
    cy.contains(`The plan ${planName}-Keyless has been closed with success.`).should('be.visible');
    cy.contains(`${planName}-Keyless`).should('not.exist');
  });

  after(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.log('Clean up APIs');
    cy.teardownApi(api);
  });
});
