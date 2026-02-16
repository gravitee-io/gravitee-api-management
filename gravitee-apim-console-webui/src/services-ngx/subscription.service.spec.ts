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

import { SubscriptionService } from './subscription.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeSubscriptionPage } from '../entities/subscription/subscription.fixture';

describe('SubscriptionService', () => {
  let httpTestingController: HttpTestingController;
  let subscriptionService: SubscriptionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    subscriptionService = TestBed.inject<SubscriptionService>(SubscriptionService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getApiSubscriptionsByPlan', () => {
    it('should get the api subscriptions', done => {
      const apiId = 'API#1';
      const planId = 'PLAN#1';
      const mockSubscription = fakeSubscriptionPage();

      subscriptionService.getApiSubscriptionsByPlan(apiId, planId).subscribe(response => {
        expect(response).toMatchObject([mockSubscription]);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/subscriptions?plan=${planId}&status=accepted,pending,rejected,closed,paused`,
      });

      req.flush([mockSubscription]);
    });
  });

  describe('getApplicationSubscriptions', () => {
    it('should get the application subscriptions', done => {
      const appId = 'APP#1';
      const mockSubscription = fakeSubscriptionPage();

      subscriptionService.getApplicationSubscriptions(appId).subscribe(response => {
        expect(response).toMatchObject([mockSubscription]);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${appId}/subscriptions?expand=security`,
      });

      req.flush([mockSubscription]);
    });
  });
});
