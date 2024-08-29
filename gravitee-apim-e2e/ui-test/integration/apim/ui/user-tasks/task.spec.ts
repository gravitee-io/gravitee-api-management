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
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { PlanSecurityType, PlanStatus, PlanValidationType, ReviewAction } from '@gravitee/management-webclient-sdk/src/lib/models';
import { ApplicationsFaker } from '@gravitee/fixtures/management/ApplicationsFaker';
import { ApplicationEntity } from '@model/applications';
import faker, { Faker } from '@faker-js/faker';
import { ApiImport } from '@model/api-imports';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('Task screen', () => {
  describe('Go to tasks by clicking on profile picture', () => {
    it('should open Task page with no tasks to be done', () => {
      cy.loginInAPIM(API_PUBLISHER_USER.username, API_PUBLISHER_USER.password);
      cy.visit('/');
      cy.getByDataTestId('top-nav-user-menu-button').click();
      cy.getByDataTestId('user-menu-task-button').click();
      cy.url().should('include', 'tasks');
      cy.get('.mat-h5').contains('No tasks to display.');
      cy.get('mat-icon[svgicon="gio:sun"]').should('be.visible');
    });
  });

  describe('Check appearance for different kind of tasks', () => {
    beforeEach(() => {
      cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
      cy.visit(`/#!/${envId}/tasks`);
      cy.url().should('include', 'tasks');
    });

    describe('API reviews', () => {
      let api: ApiImport;

      before(() => {
        // create API
        cy.request({
          method: 'POST',
          url: `${Cypress.env('managementApi')}/management/organizations/${orgId}/environments/${envId}/apis/import`,
          auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
          body: ApisFaker.apiImport({
            plans: [PlansFaker.plan({ status: PlanStatus.PUBLISHED })],
          }),
        }).then((response) => {
          expect(response.status).to.eq(200);
          api = response.body;

          // change API review state to 'ASK'
          cy.request({
            method: 'POST',
            url: `${Cypress.env('managementApi')}/management/organizations/${orgId}/environments/${envId}/apis/${api.id}/reviews`,
            auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
            body: { message: 'my test message' },
            qs: { action: ReviewAction.ASK },
          }).then((response) => {
            expect(response.status).to.eq(204);
          });
        });
      });

      it('should create an API review entry in Tasks', () => {
        cy.get('h2').contains('My Tasks (1)');
        cy.getByDataTestId('task-card')
          .should('contain', 'API review')
          .should('contain', `The API ${api.name} is ready to be reviewed`)
          .find('button')
          .its(0)
          .should('have.text', 'Review');
      });

      after(() => {
        cy.clearCookie('Auth-Graviteeio-APIM');
        cy.teardownApi(api);
      });
    });

    describe('Subscription validation', () => {
      let api: ApiImport;
      let application: ApplicationEntity;

      before(() => {
        // create API with key plan and manual validation
        cy.request({
          method: 'POST',
          url: `${Cypress.env('managementApi')}/management/organizations/${orgId}/environments/${envId}/apis/import`,
          auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
          body: ApisFaker.apiImport({
            plans: [
              PlansFaker.plan({ security: PlanSecurityType.API_KEY, status: PlanStatus.PUBLISHED, validation: PlanValidationType.MANUAL }),
            ],
          }),
        }).then((response) => {
          expect(response.status).to.eq(200);
          api = response.body;

          // create an application to subscribe to the API Key plan
          cy.request({
            method: 'POST',
            url: `${Cypress.env('managementApi')}/management/organizations/${orgId}/environments/${envId}/applications`,
            auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
            body: ApplicationsFaker.newApplication(),
          }).then((response) => {
            expect(response.status).to.eq(201);
            application = response.body;

            // subscribe application to plan
            cy.request({
              method: 'POST',
              url: `${Cypress.env('managementApi')}/management/organizations/${orgId}/environments/${envId}/applications/${
                application.id
              }/subscriptions`,
              auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
              qs: {
                plan: api.plans[0].id,
              },
            }).then((response) => {
              expect(response.status).to.eq(201);
            });
          });
        });
      });

      it('should create a validate subscription entry in Tasks', () => {
        cy.get('h2').contains('My Tasks (1)');
        cy.getByDataTestId('task-card')
          .should('contain', 'Subscription')
          .should(
            'contain',
            `The application ${application.name} requested a subscription for API ${api.name} (plan: ${api.plans[0].name})`,
          )
          .find('button')
          .its(0)
          .should('have.text', 'Validate');
      });

      after(() => {
        cy.clearCookie('Auth-Graviteeio-APIM');
        cy.teardownApi(api);
      });
    });

    describe('User registration', () => {
      const email = faker.internet.email();
      const firstname = faker.name.firstName();
      const lastname = faker.name.lastName();
      let userId: string;

      before(() => {
        cy.request({
          method: 'POST',
          url: `${Cypress.env('managementApi')}/management/organizations/${orgId}/settings`,
          auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
          body: {
            management: {
              automaticValidation: false,
            },
          },
        }).then((response) => {
          expect(response.status).to.eq(200);

          cy.request({
            method: 'POST',
            url: `${Cypress.env('managementApi')}/management/organizations/${orgId}/users/registration`,
            body: { email, firstname, lastname },
          }).then((response) => {
            expect(response.status).to.eq(200);
            userId = response.body.id;
          });
        });
      });

      it('should create a user validation entry in Tasks', () => {
        cy.get('h2').contains('My Tasks (1)');
        cy.getByDataTestId('task-card')
          .should('contain', 'User registration')
          .should('contain', `The registration of the user ${firstname} ${lastname} has to be validated`)
          .find('button')
          .its(0)
          .should('have.text', 'Validate');
      });

      after(() => {
        cy.clearCookie('Auth-Graviteeio-APIM');
        cy.request({
          method: 'DELETE',
          auth: { username: ADMIN_USER.username, password: ADMIN_USER.password },
          url: `${Cypress.env('managementApi')}/management/organizations/${orgId}/users/${userId}`,
        });
      });
    });
  });
});
