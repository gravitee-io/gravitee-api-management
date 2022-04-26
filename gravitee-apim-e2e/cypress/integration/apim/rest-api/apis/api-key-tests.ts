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
const jwt = require('jsonwebtoken');
import { ADMIN_USER, API_PUBLISHER_USER } from '@fakers/users/users';
import { deleteApi, deployApi, getApiKeys, importCreateApi, startApi, stopApi } from '@commands/management/api-management-commands';
import { publishPlan, closePlan } from '@commands/management/api-plan-management-commands';
import { PlanSecurityType } from '@model/plan';
import { ApiImportFakers } from '@fakers/api-imports';
import { ApiImport } from '@model/api-imports';
import {
  closeApplicationSubscription,
  createApplication,
  deleteApplication,
  subscribeApplication,
} from '@commands/management/application-management-commands';
import { ApplicationFakers } from '@fakers/applications';
import { requestGateway } from 'support/common/http.commands';

context('Create and test an API Key plan', () => {
  let createdApi: ApiImport;
  let applicationId: string;
  let planId: string;
  let subscriptionId: string;
  let apiKey: string;

  before(() => {
    const fakePlan = ApiImportFakers.plan({ security: PlanSecurityType.API_KEY });
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
        const fakeApplication = ApplicationFakers.application();
        createApplication(API_PUBLISHER_USER, fakeApplication).created().its('body');
      })
      .then((application) => {
        applicationId = application.id;
        subscribeApplication(API_PUBLISHER_USER, applicationId, planId)
          .created()
          .then((response) => (subscriptionId = response.body.id));
      })
      .then(() => {
        deployApi(API_PUBLISHER_USER, createdApi.id).ok();
        startApi(API_PUBLISHER_USER, createdApi.id).noContent();
        getApiKeys(API_PUBLISHER_USER, createdApi.id, subscriptionId)
          .ok()
          .then((response) => (apiKey = response.body[0].key));
      });
  });

  it('should fail to call API endpoint without an API-Key', () => {
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
      .unauthorized()
      .should((response: Cypress.Response<any>) => {
        expect(response.body.message).to.equal('Unauthorized');
      });
  });

  it('should succeed to call API endpoint with API-Key in query parameter', () => {
    requestGateway(
      {
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${createdApi.context_path}?api-key=${apiKey}`,
      },
      {
        validWhen: (response) => {
          return response.status === 200;
        },
      },
    )
      .ok()
      .should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('date');
        expect(response.body).to.have.property('timestamp');
      });
  });

  it('should succeed to call API endpoint with API-Key in header', () => {
    requestGateway(
      {
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${createdApi.context_path}`,
        headers: {
          'x-gravitee-api-key': `${apiKey}`,
        },
      },
      {
        validWhen: (response) => {
          return response.status === 200;
        },
      },
    )
      .ok()
      .should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('date');
        expect(response.body).to.have.property('timestamp');
      });
  });

  after(() => {
    closeApplicationSubscription(ADMIN_USER, applicationId, subscriptionId).ok();
    deleteApplication(API_PUBLISHER_USER, applicationId).noContent();
    closePlan(API_PUBLISHER_USER, createdApi.id, planId).ok();
    stopApi(API_PUBLISHER_USER, createdApi.id).noContent();
    deleteApi(API_PUBLISHER_USER, createdApi.id).noContent();
  });
});
