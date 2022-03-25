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
import { deleteApi, deployApi, importSwaggerApi } from '@commands/management/api-management-commands';
import { getPages } from '@commands/management/api-pages-management-commands';
import { ApiImport } from '@model/api-imports';
import { requestGateway } from 'support/common/http.commands';
import swaggerv2 from 'fixtures/json/petstore_swaggerv2.json';
import openavpiv3 from 'fixtures/json/petstore_openapiv3.json';

describe('API import via Swagger definition file', () => {
  const apiImportArray = [
    ['Swagger v2 file', JSON.stringify(swaggerv2)],
    ['OpenAPI v3 file', JSON.stringify(openavpiv3)],
    ['Swagger v2 URL', `${Cypress.env('localPetstore_v2')}/swagger.json`],
    ['OpenAPI v3 URL', `${Cypress.env('localPetstore_v3')}/openapi.json`],
  ];

  apiImportArray.forEach((importType) => {
    const [apiDescription, swaggerImport] = importType;

    describe(`Swagger file import (${apiDescription})`, () => {
      let apiId: string;

      afterEach(() => deleteApi(ADMIN_USER, apiId));

      it('should import API without creating a documentation', () => {
        importSwaggerApi(API_PUBLISHER_USER, swaggerImport, '2.0.0')
          .created()
          .its('body')
          .then((api) => {
            apiId = api.id;
            expect(api.id).to.be.a('string').and.not.to.be.empty;
            expect(api.visibility).to.equal('PRIVATE');
            expect(api.state).to.equal('STOPPED');
            getPages(API_PUBLISHER_USER, apiId).ok().its('body').should('have.length', 1).should('not.have.a.property', 'SWAGGER');
          });
      });

      it('should import API and create a swagger documentation', () => {
        importSwaggerApi(API_PUBLISHER_USER, swaggerImport, { with_documentation: true })
          .created()
          .its('body')
          .then((api) => {
            apiId = api.id;
            expect(api.id).to.be.a('string').and.not.to.be.empty;
            expect(api.visibility).to.equal('PRIVATE');
            expect(api.state).to.equal('STOPPED');
            getPages(API_PUBLISHER_USER, apiId)
              .ok()
              .its('body')
              .should('have.length', 2)
              .its(1)
              .should((swaggerEntry) => {
                expect(swaggerEntry).to.have.property('id').and.not.to.be.empty;
                expect(swaggerEntry).to.have.property('type', 'SWAGGER');
                expect(swaggerEntry).to.have.property('content').and.contain(swaggerImport);
              });
          });
      });

      it('should fail to import the same Swagger API again', () => {
        importSwaggerApi(API_PUBLISHER_USER, swaggerImport)
          .created()
          .its('body')
          .then((api) => {
            apiId = api.id;
            importSwaggerApi(API_PUBLISHER_USER, swaggerImport)
              .badRequest()
              .its('body.message')
              .should('equal', `The path [${api.context_path}/] is already covered by an other API.`);
          });
      });

      it('should fail when trying to import an empty file/URL', () => {
        importSwaggerApi(API_PUBLISHER_USER, '').its('status').should('equal', 500);
      });

      it('should import API and create a path (to add policies) for every declared Swagger path', () => {
        importSwaggerApi(API_PUBLISHER_USER, swaggerImport, { with_policy_paths: true })
          .created()
          .its('body')
          .then((api) => {
            apiId = api.id;
            if (apiDescription.includes('Swagger v2')) {
              deployApi(API_PUBLISHER_USER, apiId).its('body.flows').should('have.length', 20);
            } else {
              deployApi(API_PUBLISHER_USER, apiId).its('body.flows').should('have.length', 19);
            }
          });
      });
    });
  });
});

describe('Test API endpoint policies (Swagger v2)', () => {
  let mockPolicyApi: ApiImport;
  let jsonValidationPolicyApi: ApiImport;
  let noExtrasApi: ApiImport;
  let xmlValidationPolicyApi: ApiImport;
  let swaggerImport = JSON.stringify(swaggerv2);

  before(() => {
    {
      cy.log('-----  Import a swagger API without any extra options selected  -----');
      cy.createAndStartApiFromSwagger(swaggerImport).then((api) => (noExtrasApi = api));
    }

    {
      cy.log('-----  Import a swagger API with mock policies  -----');
      const swaggerImportAttributes = {
        with_policy_paths: true,
        with_policies: ['mock'],
      };
      cy.createAndStartApiFromSwagger(swaggerImport, swaggerImportAttributes).then((api) => (mockPolicyApi = api));
    }

    {
      cy.log('-----  Import a swagger API with JSON-Validation policies  -----');
      const swaggerImportAttributes = {
        with_policy_paths: true,
        with_policies: ['json-validation'],
      };
      cy.createAndStartApiFromSwagger(swaggerImport, swaggerImportAttributes).then((api) => (jsonValidationPolicyApi = api));
    }

    {
      cy.log('-----  Import a swagger API with XML-Validation policies  -----');
      const swaggerImportAttributes = {
        with_policy_paths: true,
        with_policies: ['xml-validation'],
      };
      cy.createAndStartApiFromSwagger(swaggerImport, swaggerImportAttributes).then((api) => (xmlValidationPolicyApi = api));
    }
  });

  describe('Test without any extra options selected', () => {
    after(() => cy.teardownApi(noExtrasApi));

    it('should successfully connect to API endpoint', () => {
      requestGateway({ url: `${Cypress.env('gatewayServer')}${noExtrasApi.context_path}/pet/findByStatus?status=available` })
        .its('body')
        .should('have.length', 7)
        .its('0.name')
        .should('equal', 'Cat 1');
    });
  });

  describe('Tests mock path policy', () => {
    after(() => cy.teardownApi(mockPolicyApi));

    it('should get a mocked response when trying to reach API endpoint', () => {
      requestGateway({ url: `${Cypress.env('gatewayServer')}${mockPolicyApi.context_path}/pet/findByStatus?status=available` })
        .its('body.category.name')
        .should('equal', 'Mocked string');
    });
  });

  describe('Tests JSON-Validation path policy', () => {
    after(() => cy.teardownApi(jsonValidationPolicyApi));

    it('should fail with BAD REQUEST (400) when sending data using an invalid JSON schema', () => {
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${jsonValidationPolicyApi.context_path}/pet`,
          method: 'PUT',
          body: {
            invalidProperty: 'invalid value',
          },
        },
        {
          validWhen: (response) => response.status === 400,
        },
      ).should((response) => {
        expect(response.body.message).to.equal('Bad Request');
      });
    });

    it('should successfully connect to API endpoint if JSON schema is valid', () => {
      const body = {
        id: 2,
        category: {
          id: 0,
          name: 'string',
        },
        name: 'doggie',
        photoUrls: ['string'],
        tags: [
          {
            id: 0,
            name: 'string',
          },
        ],
        status: 'available',
      };
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${jsonValidationPolicyApi.context_path}/pet`,
          method: 'PUT',
          body,
        },
        {
          validWhen: (response) => response.status === 200,
        },
      ).should((response) => {
        expect(response.body.name).to.equal('doggie');
      });
    });
  });

  describe('Tests XML-Validation path policy', () => {
    after(() => cy.teardownApi(xmlValidationPolicyApi));

    it('should fail with BAD REQUEST (400) when sending data using an invalid XML schema', () => {
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${xmlValidationPolicyApi.context_path}/pet`,
          method: 'PUT',
          headers: {
            'Content-Type': 'application/xml',
          },
          body: {
            invalidProperty: 'invalid value',
          },
        },
        {
          validWhen: (response) => response.status === 400,
        },
      ).should((response) => {
        expect(response.body.message).to.equal('Bad Request');
      });
    });

    // test not working yet, needs investigation to figure out if there's an issue with the gateway
    it.skip('should successfully connect to API endpoint if XML schema is valid', () => {
      const body = `<?xml version="1.0" encoding="UTF-8"?><Pet><id>2</id><Category><id>0</id><name>string</name></Category><name>Cat 9</name><photoUrls><photoUrl>string</photoUrl></photoUrls><tags><Tag><id>0</id><name>string</name></Tag></tags><status>available</status></Pet>`;
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${xmlValidationPolicyApi.context_path}/pet`,
          method: 'PUT',
          headers: {
            'Content-Type': 'application/xml',
          },
          body,
        },
        {
          validWhen: (response) => response.status === 200,
        },
      ).should((response) => {
        expect(response.body.name).to.equal('Cat 9');
      });
    });
  });
});
