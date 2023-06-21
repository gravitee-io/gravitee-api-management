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

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { ApiSubscriptionsResponse, fakeSubscription } from '../entities/management-api-v2';

describe('ApiSubscriptionV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiSubscriptionV2Service: ApiSubscriptionV2Service;
  const API_ID = 'api-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiSubscriptionV2Service = TestBed.inject<ApiSubscriptionV2Service>(ApiSubscriptionV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API', (done) => {
      const fakeApiSubscriptionsResponse: ApiSubscriptionsResponse = {
        data: [fakeSubscription()],
      };

      apiSubscriptionV2Service.list(API_ID).subscribe((apiSubscriptionsResponse) => {
        expect(apiSubscriptionsResponse.data).toEqual([fakeSubscription()]);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions?page=1&perPage=10`,
        method: 'GET',
      });

      req.flush(fakeApiSubscriptionsResponse);
    });

    it('should list with all query params', (done) => {
      const fakeApiSubscriptionsResponse: ApiSubscriptionsResponse = {
        data: [fakeSubscription()],
      };

      apiSubscriptionV2Service
        .list(API_ID, '1', '10', ['ACCEPTED', 'CLOSED'], ['app1', 'app2'], ['plan1', 'plan2'], 'apikey', ['plan', 'application'])
        .subscribe((apiSubscriptionsResponse) => {
          expect(apiSubscriptionsResponse.data).toEqual([fakeSubscription()]);
          done();
        });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions?page=1&perPage=10&statuses=ACCEPTED,CLOSED&applicationIds=app1,app2&planIds=plan1,plan2&apikey=apikey&expands=plan,application`,
        method: 'GET',
      });

      req.flush(fakeApiSubscriptionsResponse);
    });
  });

  describe('getById', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    it('should call API', (done) => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      apiSubscriptionV2Service.getById(API_ID, SUBSCRIPTION_ID, ['application', 'plan', 'subscribedBy']).subscribe((response) => {
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

  describe('transfer', () => {
    const SUBSCRIPTION_ID = 'my-subscription';
    const PLAN_ID = 'my-plan';
    it('should call API', (done) => {
      const subscription = fakeSubscription({ id: SUBSCRIPTION_ID });
      apiSubscriptionV2Service.transfer(API_ID, SUBSCRIPTION_ID, PLAN_ID).subscribe((response) => {
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
});
