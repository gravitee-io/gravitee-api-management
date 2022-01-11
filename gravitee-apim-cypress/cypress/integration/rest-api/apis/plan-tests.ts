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
import { ADMIN_USER } from 'fixtures/fakers/users/users';
import { ApiFakers } from 'fixtures/fakers/apis';
import { PlanFakers } from 'fixtures/fakers/plans';
import { createApi, deleteApi } from 'commands/management/api-management-commands';
import { createPlan, deletePlan, publishPlan } from 'commands/management/api-plan-management-commands';
import { PlanSecurityType } from '@model/plan';

context('Plan tests', () => {
  describe('Create a Plan', function () {
    beforeEach(function () {
      const fakeApi = ApiFakers.api();
      createApi(ADMIN_USER, fakeApi, true).its('body.id').as('apiId');
    });

    afterEach(function () {
      deletePlan(ADMIN_USER, this.apiId, this.planId).its('status').should('equal', 204);
      deleteApi(ADMIN_USER, this.apiId).its('status').should('equal', 204);
    });

    it('should create a keyless plan as admin user', function () {
      const fakePlan = PlanFakers.plan({ security: PlanSecurityType.KEY_LESS });
      createPlan(ADMIN_USER, this.apiId, fakePlan).should(function (response) {
        expect(response.status).to.equal(201);
        expect(response.body).to.have.all.keys(
          'name',
          'id',
          'description',
          'validation',
          'security',
          'type',
          'status',
          'api',
          'order',
          'created_at',
          'updated_at',
          'paths',
          'flows',
          'comment_required',
        );
        expect(response.body.security).to.equal('KEY_LESS');
        expect(response.body.status).to.equal('STAGING');
        cy.wrap(response.body.id).as('planId');
      });
    });
  });

  describe('Publish plan', function () {
    beforeEach(function () {
      const fakeApi = ApiFakers.api();
      const fakePlan = PlanFakers.plan();
      createApi(ADMIN_USER, fakeApi).should(function (response) {
        expect(response.status).to.equal(201);
        cy.wrap(response.body.id).as('apiId');
        createPlan(ADMIN_USER, response.body.id, fakePlan, true).its('body.id').as('planId');
      });
    });

    afterEach(function () {
      deletePlan(ADMIN_USER, this.apiId, this.planId).its('status').should('equal', 204);
      deleteApi(ADMIN_USER, this.apiId).its('status').should('equal', 204);
    });

    it('should publish a plan as admin user', function () {
      publishPlan(ADMIN_USER, this.apiId, this.planId).should(function (response) {
        expect(response.status).to.equal(200);
        expect(response.body.status).to.equal('PUBLISHED');
        cy.wrap(response.body.id).as('planId');
      });
    });
  });
});
