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
import { ADMIN_USER, API_PUBLISHER_USER } from 'fixtures/fakers/users/users';
import { ApiFakers } from 'fixtures/fakers/apis';
import { PlanFakers } from 'fixtures/fakers/plans';
import {
  createApi,
  deleteApi,
  deployApi,
  importCreateApi,
  importUpdateApi,
  startApi,
  stopApi,
  updateApi,
} from 'commands/management/api-management-commands';
import { createPlan, publishPlan, deletePlan } from 'commands/management/api-plan-management-commands';
import { Api, ResponseTemplate } from 'model/apis';
import { NewPlanEntity, PlanSecurityType } from 'model/plan';
import { ApiImportFakers } from 'fixtures/fakers/api-imports';
import { ApiImport } from '@model/api-imports';
import { requestGateway } from 'support/common/http.commands';

context('Create a mock policy on a API v1 (path based)', () => {
  let createdApi: Api;
  let createdPlan: NewPlanEntity;

  before(() => {
    const fakeApi: Api = ApiFakers.api();
    const fakePlan = PlanFakers.plan({ security: PlanSecurityType.KEY_LESS });
    createApi(API_PUBLISHER_USER, fakeApi)
      .created()
      .then((response) => {
        createdApi = response.body;
        createPlan(API_PUBLISHER_USER, createdApi.id, fakePlan)
          .created()
          .its('body')
          .then((plan: NewPlanEntity) => {
            createdPlan = plan;
            publishPlan(API_PUBLISHER_USER, createdApi.id, createdPlan.id).ok();
          });
      });
  });

  it('should create and deploy a mock policy (v1 API)', () => {
    cy.fixture('json/updateApi_mockPolicy_example').then((apiFixture) => {
      let mockPolicyApi = {
        name: createdApi.name,
        description: createdApi.description,
        proxy: createdApi.proxy,
        version: createdApi.version,
        visibility: createdApi.visibility,
        path_mappings: apiFixture.path_mappings,
        paths: apiFixture.paths,
      };
      updateApi(API_PUBLISHER_USER, createdApi.id, mockPolicyApi).ok();
      deployApi(API_PUBLISHER_USER, createdApi.id).ok();
      startApi(API_PUBLISHER_USER, createdApi.id).noContent();
    });
  });

  it('should successfully call the mocked API endpoint', function () {
    requestGateway(
      {
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${createdApi.context_path}`,
      },
    ).should((response: Cypress.Response<any>) => {
      expect(response.headers['test-value']).to.equal('value123');
      expect(response.body.message).to.equal('This is a mocked response');
    });
  });

  after(() => {
    deletePlan(API_PUBLISHER_USER, createdApi.id, createdPlan.id).noContent();
    stopApi(API_PUBLISHER_USER, createdApi.id).noContent();
    deleteApi(API_PUBLISHER_USER, createdApi.id).noContent();
  });
});

context('Create a mock policy (API v2)', () => {
  const fakePlan = ApiImportFakers.plan({ name: 'test plan', description: 'this is a test plan' });
  const fakeApi = ApiImportFakers.api({ plans: [fakePlan] });
  let createdApi: ApiImport;

  before(() => {
    importCreateApi(API_PUBLISHER_USER, fakeApi)
      .ok()
      .its('body')
      .then((api) => {
        createdApi = api;
        publishPlan(API_PUBLISHER_USER, createdApi.id, createdApi.plans[0].id).ok();
      });
  });

  it('should create and deploy a mock policy', () => {
    cy.fixture('json/mockPolicy_stepObject').then((flow) => {
      createdApi.plans[0].flows[0] = flow;
      importUpdateApi(API_PUBLISHER_USER, createdApi.id, createdApi).ok();
      deployApi(API_PUBLISHER_USER, createdApi.id).ok();
      startApi(API_PUBLISHER_USER, createdApi.id).noContent();
    });
  });

  it('should successfully call the mocked API endpoint', () => {
    requestGateway(
      {
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${createdApi.context_path}`,
      },
    ).should((response: Cypress.Response<any>) => {
      expect(response.body.message).to.equal('this is a mock response');
    });
  });

  after(() => {
    deletePlan(API_PUBLISHER_USER, createdApi.id, createdApi.plans[0].id).noContent();
    stopApi(API_PUBLISHER_USER, createdApi.id).noContent();
    deleteApi(API_PUBLISHER_USER, createdApi.id).noContent();
  });
});
