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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApiSubscriptionV2Service } from './api-subscription-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  AcceptSubscription,
  ApiSubscriptionsResponse,
  CreateSubscription,
  fakeSubscription,
  VerifySubscription,
} from '../entities/management-api-v2';
import { fakeApi } from '../entities/api/Api.fixture';
import { fakeApiKey } from '../entities/management-api-v2/api-key';

describe('ApiSubscriptionV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiSubscriptionV2Service: ApiSubscriptionV2Service;
  const API_ID = 'api-id';
  const PLAN_ID = 'plan-id';
  const APPLICATION_ID = 'application-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiSubscriptionV2Service = TestBed.inject<ApiSubscriptionV2Service>(ApiSubscriptionV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API', done => {
      const fakeApiSubscriptionsResponse: ApiSubscriptionsResponse = {
        data: [fakeSubscription()],
      };

      apiSubscriptionV2Service.list(API_ID).subscribe(apiSubscriptionsResponse => {
        expect(apiSubscriptionsResponse.data).toEqual([fakeSubscription()]);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions?page=1&perPage=10`,
        method: 'GET',
      });

      req.flush(fakeApiSubscriptionsResponse);
    });

    it('should list with all query params', done => {
      const fakeApiSubscriptionsResponse: ApiSubscriptionsResponse = {
        data: [fakeSubscription()],
      };

      apiSubscriptionV2Service
        .list(API_ID, '1', '10', ['ACCEPTED', 'CLOSED'], ['app1', 'app2'], ['plan1', 'plan2'], 'apiKey', ['plan', 'application'])
        .subscribe(apiSubscriptionsResponse => {
          expect(apiSubscriptionsResponse.data).toEqual([fakeSubscription()]);
          done();
        });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions?page=1&perPage=10&statuses=ACCEPTED,CLOSED&applicationIds=app1,app2&planIds=plan1,plan2&apiKey=apiKey&expands=plan,application`,
        method: 'GET',
      });

      req.flush(fakeApiSubscriptionsResponse);
    });
  });

  describe('export', () => {
    it('should call the API', done => {
      apiSubscriptionV2Service.exportAsCSV(API_ID).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/_export?page=1&perPage=10`,
        method: 'GET',
      });

      req.flush(null);
    });

    it('should export with all query params', done => {
      apiSubscriptionV2Service
        .exportAsCSV(API_ID, '1', '10', ['ACCEPTED', 'CLOSED'], ['app1', 'app2'], ['plan1', 'plan2'], 'apiKey')
        .subscribe(() => {
          done();
        });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/_export?page=1&perPage=10&statuses=ACCEPTED,CLOSED&applicationIds=app1,app2&planIds=plan1,plan2&apiKey=apiKey`,
        method: 'GET',
      });

      req.flush(null);
    });
  });

  describe('getById', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    it('should call API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      apiSubscriptionV2Service.getById(API_ID, SUBSCRIPTION_ID, ['application', 'plan', 'subscribedBy']).subscribe(response => {
        expect(response).toEqual(subscription);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}?expands=application,plan,subscribedBy`,
          method: 'GET',
        })
        .flush(subscription);
    });
  });

  describe('update', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    it('should call API', done => {
      const startDate = new Date();
      const endDate = new Date(new Date().setFullYear(2050));
      const metadata = { nice: 'metadata' };
      const consumerConfiguration = {
        entrypointId: 'entrypoint-id',
        entrypointConfiguration: {
          nice: 'config',
        },
      };

      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID, startingAt: startDate, endingAt: endDate });

      apiSubscriptionV2Service
        .update(API_ID, SUBSCRIPTION_ID, {
          startingAt: startDate,
          endingAt: endDate,
          metadata,
          consumerConfiguration,
        })
        .subscribe(response => {
          expect(response).toEqual(subscription);
          done();
        });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual({
        startingAt: startDate,
        endingAt: endDate,
        metadata,
        consumerConfiguration,
      });
      req.flush(subscription);
    });
  });

  describe('transfer', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    const PLAN_ID = 'my-plan';
    it('should call API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      apiSubscriptionV2Service.transfer(API_ID, SUBSCRIPTION_ID, PLAN_ID).subscribe(response => {
        expect(response).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/_transfer`,
        method: 'POST',
      });
      expect(req.request.body.planId).toEqual(PLAN_ID);

      req.flush(subscription);
    });
  });

  describe('pause', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    it('should call API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      apiSubscriptionV2Service.pause(SUBSCRIPTION_ID, API_ID).subscribe(response => {
        expect(response).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/_pause`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({});

      req.flush(subscription);
    });
  });

  describe('resume', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    it('should call API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      apiSubscriptionV2Service.resume(SUBSCRIPTION_ID, API_ID).subscribe(response => {
        expect(response).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/_resume`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({});

      req.flush(subscription);
    });
  });

  describe('creation', () => {
    it('should call the API', done => {
      const createSubscription: CreateSubscription = {
        applicationId: APPLICATION_ID,
        planId: PLAN_ID,
        customApiKey: 'my-custom-api-key',
        metadata: {
          key: 'value',
        },
        apiKeyMode: 'EXCLUSIVE',
      };

      apiSubscriptionV2Service.create(API_ID, createSubscription).subscribe(subscription => {
        expect(subscription).toEqual(fakeSubscription());
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions`,
        method: 'POST',
      });
      expect(req.request.body).toEqual(createSubscription);
      req.flush(fakeSubscription());
    });
  });

  describe('close', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    it('should call API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      apiSubscriptionV2Service.close(SUBSCRIPTION_ID, API_ID).subscribe(response => {
        expect(response).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/_close`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({});

      req.flush(subscription);
    });
  });

  describe('verify', () => {
    it('should call the endpoint', done => {
      const apiId = 'my-api-id';
      const verifySubscription: VerifySubscription = {
        applicationId: 'my-app-id',
        apiKey: 'my-api-key',
      };
      const mockApi = fakeApi();

      apiSubscriptionV2Service.verify(apiId, verifySubscription).subscribe(response => {
        expect(response).toMatchObject(mockApi);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/subscriptions/_verify`,
      });
      expect(req.request.body).toEqual(verifySubscription);
      req.flush(mockApi);
    });
  });

  describe('accept', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    it('should call API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      const acceptSubscription: AcceptSubscription = {
        startingAt: new Date(),
        endingAt: new Date(),
        reason: 'a really good reason',
        customApiKey: 'fresh-api-key',
      };
      apiSubscriptionV2Service.accept(SUBSCRIPTION_ID, API_ID, acceptSubscription).subscribe(response => {
        expect(response).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/_accept`,
        method: 'POST',
      });
      expect(req.request.body).toEqual(acceptSubscription);

      req.flush(subscription);
    });
  });

  describe('reject', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    it('should call API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      const reason = 'a very good reason';
      apiSubscriptionV2Service.reject(SUBSCRIPTION_ID, API_ID, reason).subscribe(response => {
        expect(response).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/_reject`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ reason: reason });

      req.flush(subscription);
    });
  });

  describe('list API Keys', () => {
    const API_KEY_ID = 'my-api-key';
    const SUBSCRIPTION_ID = 'my-subscription-id';
    it('should call API', done => {
      const apiKey = fakeApiKey({ id: API_KEY_ID });
      const apiKeyResponse = { data: [apiKey] };
      apiSubscriptionV2Service.listApiKeys(API_ID, SUBSCRIPTION_ID).subscribe(response => {
        expect(response).toEqual(apiKeyResponse);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys?page=1&perPage=10`,
          method: 'GET',
        })
        .flush(apiKeyResponse);
    });
  });

  describe('renew API Key', () => {
    const API_KEY_ID = 'my-api-key';
    const SUBSCRIPTION_ID = 'my-subscription-id';
    it('should call API', done => {
      const apiKey = fakeApiKey({ id: API_KEY_ID });
      const customApiKey = 'my-custom-api-key';
      apiSubscriptionV2Service.renewApiKey(API_ID, SUBSCRIPTION_ID, customApiKey).subscribe(response => {
        expect(response).toEqual(apiKey);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys/_renew`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ customApiKey: customApiKey });

      req.flush(apiKey);
    });
  });

  describe('revoke API Key', () => {
    const API_KEY_ID = 'my-api-key';
    const SUBSCRIPTION_ID = 'my-subscription-id';
    it('should call API', done => {
      const apiKey = fakeApiKey({ id: API_KEY_ID });
      apiSubscriptionV2Service.revokeApiKey(API_ID, SUBSCRIPTION_ID, API_KEY_ID).subscribe(response => {
        expect(response).toEqual(apiKey);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys/${API_KEY_ID}/_revoke`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({});

      req.flush(apiKey);
    });
  });

  describe('expire API Key', () => {
    const API_KEY_ID = 'my-api-key';
    const SUBSCRIPTION_ID = 'my-subscription-id';
    it('should call API', done => {
      const apiKey = fakeApiKey({ id: API_KEY_ID });
      const expireAt = new Date();
      apiSubscriptionV2Service.expireApiKey(API_ID, SUBSCRIPTION_ID, API_KEY_ID, expireAt).subscribe(response => {
        expect(response).toEqual(apiKey);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys/${API_KEY_ID}`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual({ expireAt });

      req.flush(apiKey);
    });
  });

  describe('reactivate API Key', () => {
    const API_KEY_ID = 'my-api-key';
    const SUBSCRIPTION_ID = 'my-subscription-id';
    it('should call API', done => {
      const apiKey = fakeApiKey({ id: API_KEY_ID });
      apiSubscriptionV2Service.reactivateApiKey(API_ID, SUBSCRIPTION_ID, API_KEY_ID).subscribe(response => {
        expect(response).toEqual(apiKey);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys/${API_KEY_ID}/_reactivate`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({});

      req.flush(apiKey);
    });
  });
});
