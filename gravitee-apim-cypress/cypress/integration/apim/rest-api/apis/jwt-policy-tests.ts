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
import { sign } from 'jsonwebtoken';
import { API_PUBLISHER_USER } from '@fakers/users/users';
import { deleteApi, deployApi, getApiKeys, importCreateApi, startApi, stopApi } from '@commands/management/api-management-commands';
import { publishPlan, closePlan } from '@commands/management/api-plan-management-commands';
import { PlanSecurityType } from '@model/plan';
import { ApiImportFakers } from '@fakers/api-imports';
import { ApiImport } from '@model/api-imports';
import { requestGateway } from 'support/common/http.commands';
import * as faker from 'faker';
import { PolicyFakers } from '@fakers/policies';

context('Create and test JWT policy', () => {
  let createdApi: ApiImport;
  let planId: string;
  const secret = faker.random.alpha({ count: 32 });
  const jwtToken_hs256 = sign({ exp: 1900000000 }, secret, { noTimestamp: true });
  const jwtToken_expired = sign({ exp: 1600000000 }, secret, { noTimestamp: true });

  before(() => {
    const fakeJwtPolicy = PolicyFakers.jwtPolicy(secret);
    const fakeJwtFlow = ApiImportFakers.flow({ pre: [fakeJwtPolicy] });
    const fakePlan = ApiImportFakers.plan({ security: PlanSecurityType.KEY_LESS, flows: [fakeJwtFlow] });
    const fakeApi = ApiImportFakers.api({ plans: [fakePlan] });
    importCreateApi(API_PUBLISHER_USER, fakeApi)
      .ok()
      .its('body')
      .then((api) => {
        createdApi = api;
        planId = createdApi.plans[0].id;
        publishPlan(API_PUBLISHER_USER, createdApi.id, planId).ok();
      })
      .then(() => {
        deployApi(API_PUBLISHER_USER, createdApi.id).ok();
        startApi(API_PUBLISHER_USER, createdApi.id).noContent();
      });
  });

  it('should successfully call API endpoint when using JWT token', () => {
    cy.log('JWT access token (HS256): ', jwtToken_hs256);
    requestGateway({
      method: 'GET',
      url: `${Cypress.env('gatewayServer')}${createdApi.context_path}`,
      headers: {
        Authorization: `Bearer ${jwtToken_hs256}`,
      },
    }).should((response: Cypress.Response<any>) => {
      expect(response.body).to.have.property('date');
      expect(response.body).to.have.property('timestamp');
    });
  });

  it('should fail to call API endpoint without JWT token', () => {
    requestGateway(
      {
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${createdApi.context_path}`,
      },
      {
        validWhen: (response) => {
          return response.status === 401;
        },
      },
    )
      .its('body')
      .should('have.property', 'message')
      .and('contain', 'Unauthorized');
  });

  it('should fail to call API endpoint when using an expired JWT token', () => {
    cy.log('JWT access token (RS256): ', jwtToken_expired);
    requestGateway(
      {
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${createdApi.context_path}`,
        headers: {
          Authorization: `Bearer ${jwtToken_expired}`,
        },
      },
      {
        validWhen: (response) => {
          return response.status === 401;
        },
      },
    )
      .its('body')
      .should('have.property', 'message')
      .and('contain', 'Unauthorized');
  });

  it('should fail to call API endpoint when using invalid JWT token', () => {
    const invalidToken = jwtToken_hs256.slice(0, -1);
    requestGateway(
      {
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${createdApi.context_path}`,
        headers: {
          Authorization: `Bearer ${invalidToken}`,
        },
      },
      {
        validWhen: (response) => {
          return response.status === 401;
        },
      },
    )
      .its('body')
      .should('have.property', 'message')
      .and('contain', 'Unauthorized');
  });

  after(() => {
    closePlan(API_PUBLISHER_USER, createdApi.id, planId).ok();
    stopApi(API_PUBLISHER_USER, createdApi.id).noContent();
    deleteApi(API_PUBLISHER_USER, createdApi.id).noContent();
  });
});
