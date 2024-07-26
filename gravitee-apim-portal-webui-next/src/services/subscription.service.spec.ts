/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { SubscriptionStatusEnum } from '../entities/subscription/subscription';
import { fakeSubscriptionResponse } from '../entities/subscription/subscription.fixture';
import { SubscriptionsResponse } from '../entities/subscription/subscriptions-response';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('SubscriptionService', () => {
  let service: SubscriptionService;
  let httpTestingController: HttpTestingController;
  const apiId = 'testId';
  const status: SubscriptionStatusEnum[] = ['PENDING'];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(SubscriptionService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should return subscription list', done => {
    const subscriptionResponse: SubscriptionsResponse = fakeSubscriptionResponse();
    service.list({ apiId, statuses: status }).subscribe(response => {
      expect(response).toMatchObject(subscriptionResponse);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions?apiId=testId&statuses=PENDING`);
    expect(req.request.method).toEqual('GET');

    req.flush(subscriptionResponse);
  });
});
