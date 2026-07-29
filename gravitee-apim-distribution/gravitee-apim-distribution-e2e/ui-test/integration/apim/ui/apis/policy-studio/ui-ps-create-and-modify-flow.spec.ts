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
import { HttpListener, MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { ApiType, ApiV4 } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import PolicyStudio from 'ui-test/support/PageObjects/Apis/PolicyStudio';
import { faker } from '@faker-js/faker';

const envId = 'DEFAULT';

describe('Create and modify a flow in Policy Studio', () => {
  let v4api: ApiV4;
  const httpListener: HttpListener = MAPIV2ApisFaker.newHttpListener();
  const apiPath: string = httpListener.paths[0].path;
  const flowName = faker.lorem.words(3);

  before(() => {
    cy.log('Create v4 API');
    cy.request({
      method: 'POST',
      url: `${Cypress.env('managementApi')}/management/v2/environments/DEFAULT/apis`,
      auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
      body: MAPIV2ApisFaker.newApi({
        type: ApiType.PROXY,
        listeners: [httpListener],
        endpointGroups: [MAPIV2ApisFaker.newHttpEndpointGroup()],
      }),
    }).then((response) => {
      expect(response.status).to.eq(201);
      v4api = response.body;

      cy.log('Create a plan with a flow');
      cy.request({
        method: 'POST',
        url: `${Cypress.env('managementApi')}/management/v2/environments/${envId}/apis/${v4api.id}/plans`,
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
            url: `${Cypress.env('managementApi')}/management/v2/environments/DEFAULT/apis/${v4api.id}/plans/${planId}/_publish`,
            auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
          }).then((response) => {
            expect(response.status).to.eq(200);
          });
        });

      cy.log('Deploy API');
      cy.request({
        method: 'POST',
        url: `${Cypress.env('managementApi')}/management/v2/environments/${envId}/apis/${v4api.id}/deployments`,
        auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
      }).then((response) => {
        expect(response.status).to.eq(202);
      });

      cy.log('Start API');
      cy.request({
        method: 'POST',
        url: `${Cypress.env('managementApi')}/management/v2/environments/${envId}/apis/${v4api.id}/_start`,
        auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
      }).then((response) => {
        expect(response.status).to.eq(204);
      });
    });
  });

  beforeEach(() => {
    cy.loginInAPIM(API_PUBLISHER_USER.username, API_PUBLISHER_USER.password);
  });

  after(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.teardownV4Api(v4api.id);
  });

  describe('Verify that flows can be created and modified in Policy Studio', () => {
    const headerKey = `x-${faker.lorem.word()}`;
    const headerValue = faker.lorem.word();
    const flowPath = faker.lorem.word();

    it('should create a common flow using the "+" icon', () => {
      PolicyStudio.openPolicyStudio(v4api.id, true)
        .addCommonFlow()
        .enterName(flowName)
        .clickOnCreateButton()
        .addResponsePhasePolicy()
        .choosePolicy('Transform Headers')
        .addOrUpdateHeaders(headerKey, headerValue)
        .clickOnAddPolicyButton()
        .clickOnSaveButton()
        .deployApiUsingUi();
    });

    it('should show newly added header when calling Gateway', () => {
      const headerCheckFunction = (response: Cypress.Response<any>) => {
        const expected = response.headers[headerKey] === headerValue;
        if (!expected) {
          cy.log(
            'Fail to find header: ' +
              JSON.stringify({ headerExpected: { [headerKey]: headerValue }, responseHeaders: JSON.stringify(response.headers) }),
          );
        }

        return expected;
      };

      cy.callGateway(apiPath, headerCheckFunction);
    });

    it('should edit flow details of a common flow using pen icon', () => {
      PolicyStudio.openPolicyStudio(v4api.id, true)
        .editFlowDetails(flowName)
        .setPath(flowPath)
        .clickOnSaveButtonInDialog()
        .clickOnSaveButton()
        .deployApiUsingUi();
    });

    it('should not have added header anymore when calling Gateway as before', () => {
      const headerCheckFunction = (response: Cypress.Response<any>) => {
        return !response.headers[headerKey];
      };

      cy.callGateway(apiPath, headerCheckFunction);
    });

    it('should have header when calling Gateway with updated path', () => {
      const headerCheckFunction = (response: Cypress.Response<any>) => {
        return response.headers[headerKey] === headerValue;
      };

      cy.callGateway(`${apiPath}/${flowPath}`, headerCheckFunction);
    });
  });
});
