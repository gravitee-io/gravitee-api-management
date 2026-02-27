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

import { ApiProductSubscriptionV2Service } from './api-product-subscription-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  AcceptSubscription,
  ApiSubscriptionsResponse,
  CreateSubscription,
  fakeSubscription,
  VerifySubscription,
} from '../entities/management-api-v2';
import { fakeApiKey } from '../entities/management-api-v2/api-key';

describe('ApiProductSubscriptionV2Service', () => {
  let httpTestingController: HttpTestingController;
  let service: ApiProductSubscriptionV2Service;
  const API_PRODUCT_ID = 'api-product-id';
  const SUBSCRIPTION_ID = 'subscription-id';
  const PLAN_ID = 'plan-id';
  const APPLICATION_ID = 'application-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ApiProductSubscriptionV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API with defaults', done => {
      const response: ApiSubscriptionsResponse = { data: [fakeSubscription()] };

      service.list(API_PRODUCT_ID).subscribe(r => {
        expect(r.data).toEqual([fakeSubscription()]);
        done();
      });

      const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions`;
      const req = httpTestingController.expectOne(
        r =>
          r.method === 'GET' &&
          r.url.startsWith(baseUrl) &&
          !r.url.includes('_export') &&
          r.params.get('page') === '1' &&
          r.params.get('perPage') === '10',
      );
      req.flush(response);
    });

    it('should list with all query params', done => {
      const response: ApiSubscriptionsResponse = { data: [fakeSubscription()] };

      service
        .list(API_PRODUCT_ID, '1', '10', ['ACCEPTED', 'CLOSED'], ['app1', 'app2'], ['plan1', 'plan2'], 'my-key', ['plan', 'application'])
        .subscribe(r => {
          expect(r).toEqual(response);
          done();
        });

      const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions`;
      const req = httpTestingController.expectOne(r => {
        if (r.method !== 'GET' || !r.url.startsWith(baseUrl) || r.url.includes('_export')) return false;
        const p = r.params;
        return (
          p.get('page') === '1' &&
          p.get('perPage') === '10' &&
          p.get('statuses') === 'ACCEPTED,CLOSED' &&
          p.get('applicationIds') === 'app1,app2' &&
          p.get('planIds') === 'plan1,plan2' &&
          p.get('apiKey') === 'my-key' &&
          p.get('expands') === 'plan,application'
        );
      });
      req.flush(response);
    });
  });

  describe('getById', () => {
    it('should call the API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });

      service.getById(API_PRODUCT_ID, SUBSCRIPTION_ID, ['plan', 'application']).subscribe(r => {
        expect(r).toEqual(subscription);
        done();
      });

      const url = `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}`;
      const req = httpTestingController.expectOne(
        r => r.method === 'GET' && r.url.startsWith(url) && r.params.get('expands') === 'plan,application',
      );
      req.flush(subscription);
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      const createSubscription: CreateSubscription = { planId: PLAN_ID, applicationId: APPLICATION_ID };
      const subscription = fakeSubscription();

      service.create(API_PRODUCT_ID, createSubscription).subscribe(r => {
        expect(r).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions`,
        method: 'POST',
      });
      expect(req.request.body).toEqual(createSubscription);
      req.flush(subscription);
    });
  });

  describe('close', () => {
    it('should call the API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });

      service.close(SUBSCRIPTION_ID, API_PRODUCT_ID).subscribe(r => {
        expect(r).toEqual(subscription);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/_close`,
          method: 'POST',
        })
        .flush(subscription);
    });
  });

  describe('accept', () => {
    it('should call the API', done => {
      const acceptSubscription: AcceptSubscription = { reason: 'ok' };
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });

      service.accept(SUBSCRIPTION_ID, API_PRODUCT_ID, acceptSubscription).subscribe(r => {
        expect(r).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/_accept`,
        method: 'POST',
      });
      expect(req.request.body).toEqual(acceptSubscription);
      req.flush(subscription);
    });
  });

  describe('reject', () => {
    it('should call the API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });

      service.reject(SUBSCRIPTION_ID, API_PRODUCT_ID, 'Not approved').subscribe(r => {
        expect(r).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/_reject`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ reason: 'Not approved' });
      req.flush(subscription);
    });
  });

  describe('pause', () => {
    it('should call the API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });

      service.pause(SUBSCRIPTION_ID, API_PRODUCT_ID).subscribe(r => {
        expect(r).toEqual(subscription);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/_pause`,
          method: 'POST',
        })
        .flush(subscription);
    });
  });

  describe('resume', () => {
    it('should call the API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });

      service.resume(SUBSCRIPTION_ID, API_PRODUCT_ID).subscribe(r => {
        expect(r).toEqual(subscription);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/_resume`,
          method: 'POST',
        })
        .flush(subscription);
    });
  });

  describe('transfer', () => {
    it('should call the API', done => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      const newPlanId = 'new-plan-id';

      service.transfer(API_PRODUCT_ID, SUBSCRIPTION_ID, newPlanId).subscribe(r => {
        expect(r).toEqual(subscription);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/_transfer`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ planId: newPlanId });
      req.flush(subscription);
    });
  });

  describe('verify', () => {
    it('should call the API', done => {
      const verifySubscription: VerifySubscription = { applicationId: APPLICATION_ID, apiKey: 'my-api-key' };

      service.verify(API_PRODUCT_ID, verifySubscription).subscribe(r => {
        expect(r).toBeDefined();
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/_verify`,
        method: 'POST',
      });
      expect(req.request.body).toEqual(verifySubscription);
      req.flush({ ok: true });
    });
  });

  describe('listApiKeys', () => {
    it('should call the API', done => {
      const apiKey = fakeApiKey();

      service.listApiKeys(API_PRODUCT_ID, SUBSCRIPTION_ID).subscribe(r => {
        expect(r.data).toContain(apiKey);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys?page=1&perPage=10`,
          method: 'GET',
        })
        .flush({ data: [apiKey] });
    });
  });

  describe('renewApiKey', () => {
    it('should call the API', done => {
      const apiKey = fakeApiKey();

      service.renewApiKey(API_PRODUCT_ID, SUBSCRIPTION_ID, 'custom-key').subscribe(r => {
        expect(r).toEqual(apiKey);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys/_renew`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ customApiKey: 'custom-key' });
      req.flush(apiKey);
    });
  });

  describe('revokeApiKey', () => {
    it('should call the API', done => {
      const apiKey = fakeApiKey();

      service.revokeApiKey(API_PRODUCT_ID, SUBSCRIPTION_ID, apiKey.id).subscribe(r => {
        expect(r).toEqual(apiKey);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys/${apiKey.id}/_revoke`,
          method: 'POST',
        })
        .flush(apiKey);
    });
  });

  describe('expireApiKey', () => {
    it('should call the API', done => {
      const apiKey = fakeApiKey();
      const expireAt = new Date('2025-12-31');

      service.expireApiKey(API_PRODUCT_ID, SUBSCRIPTION_ID, apiKey.id, expireAt).subscribe(r => {
        expect(r).toEqual(apiKey);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys/${apiKey.id}`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual({ expireAt });
      req.flush(apiKey);
    });
  });

  describe('reactivateApiKey', () => {
    it('should call the API', done => {
      const apiKey = fakeApiKey();

      service.reactivateApiKey(API_PRODUCT_ID, SUBSCRIPTION_ID, apiKey.id).subscribe(r => {
        expect(r).toEqual(apiKey);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys/${apiKey.id}/_reactivate`,
          method: 'POST',
        })
        .flush(apiKey);
    });
  });
});
