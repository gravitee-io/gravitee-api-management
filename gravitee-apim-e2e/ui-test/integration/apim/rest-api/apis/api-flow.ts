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
import { ADMIN_USER, LOW_PERMISSION_USER } from '@fakers/users/users';
import { ApiFakers } from '@fakers/apis';
import { PlanFakers } from '@fakers/plans';
import { createApi, deleteApi, publishApi, startApi, stopApi } from '@commands/management/api-management-commands';
import { createPlan, publishPlan, deletePlan } from '@commands/management/api-plan-management-commands';
import { Api } from '@model/apis';
import { NewPlanEntity, PlanSecurityType } from '@model/plan';
import { requestGateway } from 'ui-test/support/common/http.commands';

context('Create an API flow', () => {
  let api: Api;
  let plan: NewPlanEntity;

  describe('Create an API', function () {
    it('should create an API as admin user', function () {
      const fakeApi: Api = ApiFakers.api();
      createApi(ADMIN_USER, fakeApi).should((response) => {
        expect(response.body.state).equal('STOPPED');
        expect(response.body.visibility).equal('PRIVATE');
        expect(response.body.lifecycle_state).equal('CREATED');
        api = response.body;
      });
    });

    it('should fail to create an API if user lacks required permssions', function () {
      const fakeApi: Api = ApiFakers.api();
      createApi(LOW_PERMISSION_USER, fakeApi).should((response) => {
        expect(response.status).to.equal(403);
        expect(response.body.message).to.equal('You do not have sufficient rights to access this resource');
      });
    });
  });

  describe('Create a plan', () => {
    it('should create a keyless plan as admin user', function () {
      const fakePlan = PlanFakers.plan({ security: PlanSecurityType.KEY_LESS });
      createPlan(ADMIN_USER, api.id, fakePlan).should(function (response) {
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
        plan = response.body;
      });
    });
  });

  describe('Publish plan', function () {
    it('should publish a plan as admin user', function () {
      publishPlan(ADMIN_USER, api.id, plan.id).should(function (response) {
        expect(response.status).to.equal(200);
        expect(response.body.status).to.equal('PUBLISHED');
      });
    });
  });

  describe('Publish an API', function () {
    it('should publish an API as admin user', function () {
      publishApi(ADMIN_USER, api).should(function (response) {
        expect(response.status).to.equal(200);
        expect(response.body.lifecycle_state).to.equal('PUBLISHED');
        expect(response.body.state).to.equal('STOPPED');
        expect(response.body.visibility).to.equal('PRIVATE');
      });
    });
  });

  describe('Start an API', function () {
    it('should start an API as admin user', function () {
      startApi(ADMIN_USER, api.id).should(function (response) {
        expect(response.status).to.equal(204);
      });
    });
  });

  describe('Test newly created API', function () {
    it('should get a positive response when calling the new API endpoint', function () {
      requestGateway({
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${api.contextPath}?teststring=${api.id}`,
      })
        .ok()
        .its('body.query_params.teststring')
        .should('equal', api.id);
    });
  });

  describe('Stop an API', function () {
    it('should delete an API as admin user', function () {
      stopApi(ADMIN_USER, api.id).its('status').should('equal', 204);
    });
  });

  describe('Delete a plan', function () {
    it('should delete a plan as admin user', function () {
      deletePlan(ADMIN_USER, api.id, plan.id).its('status').should('equal', 204);
    });
  });

  describe('Delete an API', function () {
    it('should fail to delete an API as low permission user', function () {
      deleteApi(LOW_PERMISSION_USER, api.id).should((response) => {
        expect(response.status).to.be.equal(403);
        expect(response.body.message).to.be.equal('You do not have sufficient rights to access this resource');
      });
    });

    it('should delete an API as admin user', function () {
      deleteApi(ADMIN_USER, api.id).its('status').should('equal', 204);
    });

    it('should fail to delete a non-existing API as admin user', function () {
      deleteApi(ADMIN_USER, api.id).its('status').should('equal', 404);
    });
  });
});
