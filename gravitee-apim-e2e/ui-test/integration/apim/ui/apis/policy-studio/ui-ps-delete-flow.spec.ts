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
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { ApiType, ApiV4, FlowV4, HttpListener } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';

const envId = 'DEFAULT';

describe('Deleting a flow', () => {
  let v4api: ApiV4;
  const planFlow: FlowV4 = MAPIV2PlansFaker.newFlowV4();
  const commonFlow: FlowV4 = MAPIV2ApisFaker.newFlow();
  const addedPlanFlowHeader = planFlow.response[0].configuration.addHeaders[0];
  const addedCommonFlowHeader = commonFlow.response[0].configuration.addHeaders[0];
  const httpListener: HttpListener = MAPIV2ApisFaker.newHttpListener();
  const apiPath: string = httpListener.paths[0].path;

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
        flows: [commonFlow],
      }),
    }).then((response) => {
      expect(response.status).to.eq(201);
      v4api = response.body;

      cy.log('Create a plan with a flow');
      cy.request({
        method: 'POST',
        url: `${Cypress.env('managementApi')}/management/v2/environments/${envId}/apis/${v4api.id}/plans`,
        auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
        body: MAPIV2PlansFaker.newPlanV4({ flows: [planFlow] }),
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

  describe('Verify that both flows are working on the Gateway', () => {
    it('should have additional header added by API flows', () => {
      cy.callGateway(apiPath)
        .its('headers')
        .should((headers) => {
          expect(headers).to.have.property(addedCommonFlowHeader.name, addedCommonFlowHeader.value);
          expect(headers).to.have.property(addedPlanFlowHeader.name, addedPlanFlowHeader.value);
        });
    });
  });

  describe('Verify that flow deletion works in Policy Studio', () => {
    beforeEach(() => {
      cy.loginInAPIM(API_PUBLISHER_USER.username, API_PUBLISHER_USER.password);
      cy.visit(`/#!/environments/default/apis/${v4api.id}/policy-studio`);
      cy.url().should('include', '/policy-studio');
    });

    it('should delete a common flow using trash icon', () => {
      cy.contains('.list__flowsGroup__flow__name', commonFlow.name, { timeout: 60000 }).should('be.visible').click();
      cy.get('.header__configBtn__delete').click();
      cy.contains('.list__flowsGroup__flow__name', commonFlow.name).should('not.exist');
      cy.contains('button', 'Save').click();
      cy.contains('Policy Studio configuration saved').should('be.visible');
      cy.contains('.banner__wrapper__title', 'This API is out of sync').scrollIntoView().should('be.visible');
    });

    it('should delete a plan-flow using trash icon', () => {
      cy.contains('.list__flowsGroup__flow__name', planFlow.name, { timeout: 60000 }).should('be.visible').click();
      cy.get('.header__configBtn__delete').click();
      cy.contains('.list__flowsGroup__flow__name', planFlow.name).should('not.exist');
      cy.contains('button', 'Save').click();
      cy.contains('Policy Studio configuration saved').should('be.visible');
      cy.contains('.banner__wrapper__title', 'This API is out of sync').scrollIntoView().should('be.visible');
    });

    after(() => {
      cy.log('Deploy API');
      cy.contains('button', 'Deploy API').click();
      cy.get('.mat-raised-button > .mat-button-wrapper').click();
    });
  });

  describe('Verify that flow is no longer working on the Gateway', () => {
    it(
      'should no longer have additional header from API flows after flows deleted',
      {
        retries: 5, // deployment can take some time
      },
      () => {
        cy.callGateway(apiPath)
          .its('headers')
          .should((responseHeader) => {
            expect(responseHeader).to.not.have.property(addedCommonFlowHeader.name);
            expect(responseHeader).to.not.have.property(addedPlanFlowHeader.name);
          });
      },
    );
  });

  after(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.teardownV4Api(v4api.id);
  });
});
