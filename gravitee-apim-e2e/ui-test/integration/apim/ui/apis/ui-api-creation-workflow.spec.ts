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
import faker from '@faker-js/faker';

describe('API creation workflow', () => {
  const apiVersion = `${faker.datatype.number({ min: 1, max: 9 })}.${faker.datatype.number({ min: 1, max: 9 })}`;
  const apiPath = `${faker.commerce.productName().replace(/ /g, '_')}`;
  const apiName = faker.commerce.productName();
  let apiId: string;

  describe('Create a V4 Proxy REST API', function () {
    before(() => {
      cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
      cy.visit('/#!/default/apis/new');
      cy.getByDataTestId('api_create_v4_button').should('be.visible');
    });

    it(`should create V4 API using the default API creation workflow`, () => {
      cy.getByDataTestId('api_create_v4_button').click();

      // Step 1
      cy.contains('Step 1');
      cy.getByDataTestId('api_creation_v4_validate_button').should('be.disabled');
      cy.getByDataTestId('api_creation_v4_name_input').type(apiName);
      cy.getByDataTestId('api_creation_v4_validate_button').should('be.disabled');
      cy.getByDataTestId('api_creation_v4_version_input').type(apiVersion);
      cy.getByDataTestId('api_creation_v4_validate_button').click();

      // Select Proxy Upstream Protocol option (entrypoints)`
      cy.contains('Step 2');
      cy.contains('Select how you want your backend service exposed');
      cy.getByDataTestId('select_architecture_button').should('be.disabled');
      cy.getByDataTestId('api_creation_proxy_checkbox').click();
      cy.getByDataTestId('select_architecture_button').click();

      // Step 2 (entrypoints)
      cy.contains('Step 2');
      cy.contains('Configure your API entrypoints');
      cy.getByDataTestId('validate_entrypoints_button').should('be.disabled');
      cy.get('input[formcontrolname=path]').type(apiPath);
      cy.getByDataTestId('validate_entrypoints_button').click();

      // Step 3 (endpoints)
      cy.contains('Step 3');
      const targetUrl = `${Cypress.env('wiremockUrl')}/hello`;
      cy.getByDataTestId('validate_endpoint_button').should('be.disabled');
      cy.get('input[id*=target]').should('be.enabled').type(targetUrl);
      cy.getByDataTestId('validate_endpoint_button').click();
      cy.contains('Step 4');

      // Step 4 (plans)
      cy.getByDataTestId('validate_plans_button').click();
      cy.contains('Review your API configuration');

      // Final step (summary)
      cy.intercept('POST', '**/apis').as('createApi');
      cy.getByDataTestId('deploy_api_button').click();

      // Retrieve API-id for clean-up
      cy.wait('@createApi').then((interception) => {
        expect(interception.response.statusCode).to.eq(201);
        expect(interception.response.body).to.have.property('id');
        expect(interception.response.body).to.have.property('name', apiName);
        expect(interception.response.body).to.have.property('apiVersion', apiVersion);
        apiId = interception.response.body.id;
        cy.contains(`${apiName} has been created`);

        // open new API in API Management
        cy.getByDataTestId('open_api_in_api_management_button').click();
        cy.url().should('include', `/${apiId}`);
      });
    });

    it('should successfully connect to created API via Gateway', function () {
      cy.callGateway(`/${apiPath}`).then((response) => {
        expect(response.body.message).to.eq('Hello, World!'); // from wiremock
      });
    });

    after(() => {
      cy.clearCookie('Auth-Graviteeio-APIM');
      cy.teardownV4Api(apiId);
    });
  });
});
