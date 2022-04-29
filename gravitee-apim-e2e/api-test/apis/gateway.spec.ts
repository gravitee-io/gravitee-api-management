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
import { afterAll, beforeAll, describe, expect, test } from '@jest/globals';
import { createAndStartApiFromSwagger, teardownApi } from '../../lib/jest-utils';
import { Type } from '@management-models/Type';
import { Format } from '@management-models/Format';
import * as swaggerv2 from '../resources/petstore_swaggerv2.json';
import { fetchGateway } from '../../lib/gateway';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsApiUser } from '@client-conf/*';
import { APIPagesApi } from '@management-apis/APIPagesApi';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const definitionVersion = '2.0.0';

describe('Test API endpoint policies (Swagger v2 only)', () => {
  let mockPolicyApi;
  let jsonValidationPolicyApi;
  let noExtrasApi;
  let xmlValidationPolicyApi;
  let pathMappingApi;
  let requestValidationApi;

  beforeAll(async () => {
    noExtrasApi = await createAndStartApiFromSwagger({
      orgId,
      envId,
      definitionVersion,
      importSwaggerDescriptorEntity: {
        type: Type.INLINE,
        format: Format.API,
        payload: JSON.stringify(swaggerv2),
        with_path_mapping: false,
      },
    });
    //
    // mockPolicyApi = await createAndStartApiFromSwagger({
    //   orgId,
    //   envId,
    //   definitionVersion,
    //   importSwaggerDescriptorEntity: {
    //     type: Type.INLINE,
    //     format: Format.API,
    //     payload: swaggerv2,
    //     with_policy_paths: true,
    //     with_policies: ['mock'],
    //   },
    // });
    //
    // jsonValidationPolicyApi = await createAndStartApiFromSwagger({
    //   orgId,
    //   envId,
    //   definitionVersion,
    //   importSwaggerDescriptorEntity: {
    //     type: Type.INLINE,
    //     format: Format.API,
    //     payload: swaggerv2,
    //     with_policy_paths: true,
    //     with_policies: ['json-validation'],
    //   },
    // });
    //
    // xmlValidationPolicyApi = await createAndStartApiFromSwagger({
    //   orgId,
    //   envId,
    //   definitionVersion,
    //   importSwaggerDescriptorEntity: {
    //     type: Type.INLINE,
    //     format: Format.API,
    //     payload: swaggerv2,
    //     with_policy_paths: true,
    //     with_policies: ['xml-validation'],
    //   },
    // });
    //
    // pathMappingApi = await createAndStartApiFromSwagger({
    //   orgId,
    //   envId,
    //   definitionVersion,
    //   importSwaggerDescriptorEntity: {
    //     type: Type.INLINE,
    //     format: Format.API,
    //     payload: swaggerv2,
    //     with_path_mapping: true,
    //   },
    // });
    //
    // requestValidationApi = await createAndStartApiFromSwagger({
    //   orgId,
    //   envId,
    //   definitionVersion,
    //   importSwaggerDescriptorEntity: {
    //     type: Type.INLINE,
    //     format: Format.API,
    //     payload: swaggerv2,
    //     with_policy_paths: true,
    //     with_policies: ['policy-request-validation'],
    //   },
    // });
  });

  afterAll(
    async () =>
      await Promise.all([
        teardownApi(orgId, envId, noExtrasApi),
        // teardownApi(orgId, envId, mockPolicyApi),
        // teardownApi(orgId, envId, jsonValidationPolicyApi),
        // teardownApi(orgId, envId, xmlValidationPolicyApi),
        // teardownApi(orgId, envId, pathMappingApi),
        // teardownApi(orgId, envId, requestValidationApi),
      ]),
  );

  describe('Test without any extra options selected', () => {
    test('should successfully connect to API endpoint', async () => {
      const response = await fetchGateway(`${noExtrasApi.context_path}/pet/findByStatus?status=available`);
      expect(response.body).toHaveLength(7);
      expect(response.body[0].name).toEqual('Cat 1');
    });
  });

  // describe('Tests mock path policy', () => {
  //   test('should get a mocked response when trying to reach API endpoint', async () => {
  //     const response = await fetchGateway(`${mockPolicyApi.context_path}/pet/findByStatus?status=available`);
  //     // @ts-ignore
  //     expect(response.body.category).toEqual('Mocked string');
  //   });
  // });
  //
  // describe('Tests JSON-Validation path policy', () => {
  //   test('should fail with BAD REQUEST (400) when sending data using an invalid JSON schema', () => {
  //     requestGateway(
  //       {
  //         url: `${Cypress.env('gatewayServer')}${jsonValidationPolicyApi.context_path}/pet`,
  //         method: 'PUT',
  //         body: {
  //           invalidProperty: 'invalid value',
  //         },
  //       },
  //       {
  //         validWhen: (response) => response.status === 400,
  //       },
  //     ).should((response) => {
  //       expect(response.body.message).toEqual('Bad Request');
  //     });
  //   });
  //
  //   test('should successfully connect to API endpoint if JSON schema is valid', () => {
  //     const body = {
  //       id: 2,
  //       category: {
  //         id: 0,
  //         name: 'string',
  //       },
  //       name: 'doggie',
  //       photoUrls: ['string'],
  //       tags: [
  //         {
  //           id: 0,
  //           name: 'string',
  //         },
  //       ],
  //       status: 'available',
  //     };
  //     requestGateway(
  //       {
  //         url: `${Cypress.env('gatewayServer')}${jsonValidationPolicyApi.context_path}/pet`,
  //         method: 'PUT',
  //         body,
  //       },
  //       {
  //         validWhen: (response) => response.status === 200,
  //       },
  //     ).should((response) => {
  //       expect(response.body.name).toEqual('doggie');
  //     });
  //   });
  // });
  //
  // describe('Tests XML-Validation path policy', () => {
  //   test('should fail with BAD REQUEST (400) when sending data using an invalid XML schema', () => {
  //     requestGateway(
  //       {
  //         url: `${Cypress.env('gatewayServer')}${xmlValidationPolicyApi.context_path}/pet`,
  //         method: 'PUT',
  //         headers: {
  //           'Content-Type': 'application/xml',
  //         },
  //         body: {
  //           invalidProperty: 'invalid value',
  //         },
  //       },
  //       {
  //         validWhen: (response) => response.status === 400,
  //       },
  //     ).should((response) => {
  //       expect(response.body.message).toEqual('Bad Request');
  //     });
  //   });
  //
  //   // test not working yet, needs investigation to figure out if there's an issue with the gateway
  //   it.skip('should successfully connect to API endpoint if XML schema is valid', () => {
  //     const body = `<?xml version="1.0" encoding="UTF-8"?><Pet><id>2</id><Category><id>0</id><name>string</name></Category><name>Cat 9</name><photoUrls><photoUrl>string</photoUrl></photoUrls><tags><Tag><id>0</id><name>string</name></Tag></tags><status>available</status></Pet>`;
  //     requestGateway(
  //       {
  //         url: `${Cypress.env('gatewayServer')}${xmlValidationPolicyApi.context_path}/pet`,
  //         method: 'PUT',
  //         headers: {
  //           'Content-Type': 'application/xml',
  //         },
  //         body,
  //       },
  //       {
  //         validWhen: (response) => response.status === 200,
  //       },
  //     ).should((response) => {
  //       expect(response.body.name).toEqual('Cat 9');
  //     });
  //   });
  // });
  //
  // describe('Tests Path-Mapping (Analytics)', () => {
  //   before(() => {
  //     for (let petId of [1, 2, 2, 2, 5]) {
  //       requestGateway(
  //         { url: `${Cypress.env('gatewayServer')}${noExtrasApi.context_path}/pet/${petId}` },
  //         { validWhen: (response) => response.body !== 'No context-path matches the request URI.' },
  //       );
  //       requestGateway(
  //         { url: `${Cypress.env('gatewayServer')}${pathMappingApi.context_path}/pet/${petId}` },
  //         { validWhen: (response) => response.body !== 'No context-path matches the request URI.' },
  //       );
  //     }
  //   });
  //
  //   test('should have paths set up (mentioned) in API definition if path mapping chosen', () => {
  //     expect(pathMappingApi.path_mappings).to.deep.equal([
  //       '/pet/:petId',
  //       '/store/order/:orderId',
  //       '/pet',
  //       '/user/:username',
  //       '/pet/findByStatus',
  //       '/user/createWithList',
  //       '/store/inventory',
  //       '/user/login',
  //       '/user',
  //       '/user/createWithArray',
  //       '/pet/findByTags',
  //       '/store/order',
  //       '/user/logout',
  //       '/pet/:petId/uploadImage',
  //     ]);
  //   });
  //
  //   test('should not have paths set up (mentioned) in API definition if no path mapping enabled', () => {
  //     expect(noExtrasApi.path_mappings).to.deep.equal(['/']);
  //   });
  //
  //   test('should have 5 requests mapped in path /pet/:petId after requests were sent', () => {
  //     requestGateway(
  //       {
  //         url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/apis/${pathMappingApi.id}/analytics`,
  //         auth: API_PUBLISHER_USER,
  //         qs: {
  //           type: 'group_by',
  //           field: 'mapped-path',
  //           interval: 10000,
  //           from: Date.now() - 4 * 60 * 1000,
  //           to: Date.now() + 1 * 60 * 1000,
  //         },
  //       },
  //       {
  //         validWhen: (response) => response.body.values['/pet/:petId'] === 5,
  //       },
  //     ).should((response) => {
  //       expect(Object.keys(response.body.values)).to.have.lengthOf(1);
  //     });
  //   });
  //
  //   test('should not have any mapped paths in analytics response if path-mapping was not set', () => {
  //     cy.watest(5000); // some time needed to gather potential analytics data
  //     requestGateway(
  //       {
  //         url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/apis/${noExtrasApi.id}/analytics`,
  //         auth: API_PUBLISHER_USER,
  //         qs: {
  //           type: 'group_by',
  //           field: 'mapped-path',
  //           interval: 10000,
  //           from: Date.now() - 4 * 60 * 1000,
  //           to: Date.now() + 1 * 60 * 1000,
  //         },
  //       },
  //       {
  //         validWhen: (response) => response.body.values,
  //       },
  //     ).should((response) => {
  //       expect(Cypress._.isEmpty(response.body.values));
  //     });
  //   });
  // });
  //
  // describe('Tests Request-Validation policy', () => {
  //   test('should not respond with an error if a request parameter is missing but Request-Validation policy not set', () => {
  //     requestGateway({
  //       url: `${Cypress.env('gatewayServer')}${noExtrasApi.context_path}/pet/findByStatus`,
  //     }).should((response) => {
  //       expect(response.body).to.be.empty;
  //     });
  //   });
  //
  //   test('should get a Request-Validation error if a required parameter is missing', () => {
  //     requestGateway(
  //       {
  //         url: `${Cypress.env('gatewayServer')}${requestValidationApi.context_path}/pet/findByStatus`,
  //       },
  //       {
  //         validWhen: (response) => response.status === 400,
  //       },
  //     ).should((response) => {
  //       expect(response.body.message).toEqual(
  //         '{"message":"Request is not valid according to constraint rules","constraints":["status query parameter is required"]}',
  //       );
  //     });
  //   });
  //
  //   test('should successfully reach endpoint if request is valid', () => {
  //     requestGateway(
  //       {
  //         url: `${Cypress.env('gatewayServer')}${requestValidationApi.context_path}/pet/findByStatus`,
  //         qs: {
  //           status: 'available',
  //         },
  //       },
  //       {
  //         validWhen: (response) => response.status === 200,
  //       },
  //     ).should((response) => {
  //       expect(response.body[0].name).toEqual('Cat 1');
  //       expect(response.body).to.have.lengthOf(7);
  //     });
  //   });
  // });
});
