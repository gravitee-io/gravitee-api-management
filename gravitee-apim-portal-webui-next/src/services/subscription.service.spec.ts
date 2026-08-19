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
import {
  fakeApiProductSubscriptionDetails,
  fakeSubscription,
  fakeSubscriptionResponse,
  SubscriptionsResponse,
  SubscriptionStatusEnum,
} from '../entities/subscription';
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
    service.list({ apiIds: [apiId], statuses: status }).subscribe(response => {
      expect(response).toMatchObject(subscriptionResponse);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions?apiIds=testId&statuses=PENDING`);
    expect(req.request.method).toEqual('GET');

    req.flush(subscriptionResponse);
  });

  it('should filter subscriptions by API Product IDs', done => {
    const subscriptionResponse: SubscriptionsResponse = fakeSubscriptionResponse();

    service.list({ apiProductIds: ['api-product-id'], statuses: ['PENDING', 'ACCEPTED', 'PAUSED'], size: -1 }).subscribe(response => {
      expect(response).toEqual(subscriptionResponse);
      done();
    });

    const req = httpTestingController.expectOne(
      `${TESTING_BASE_URL}/subscriptions?apiProductIds=api-product-id&statuses=PENDING&statuses=ACCEPTED&statuses=PAUSED&size=-1`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(subscriptionResponse);
  });

  it('should search API and API Product subscriptions in one backend page', done => {
    const subscriptionResponse: SubscriptionsResponse = fakeSubscriptionResponse();

    service
      .list({
        referenceTypes: ['API', 'API_PRODUCT'],
        query: 'payment',
        applicationIds: ['application-id'],
        statuses: ['ACCEPTED'],
        page: 2,
        size: 20,
      })
      .subscribe(response => {
        expect(response).toEqual(subscriptionResponse);
        done();
      });

    const req = httpTestingController.expectOne(
      `${TESTING_BASE_URL}/subscriptions?referenceTypes=API&referenceTypes=API_PRODUCT&query=payment&applicationIds=application-id&statuses=ACCEPTED&size=20&page=2`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(subscriptionResponse);
  });

  it('should omit a blank subscription target query', done => {
    const subscriptionResponse: SubscriptionsResponse = fakeSubscriptionResponse();

    service.list({ referenceTypes: ['API', 'API_PRODUCT'], query: '   ', statuses: null }).subscribe(response => {
      expect(response).toEqual(subscriptionResponse);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions?referenceTypes=API&referenceTypes=API_PRODUCT`);
    expect(req.request.method).toEqual('GET');
    req.flush(subscriptionResponse);
  });

  it('should return API Product subscription details', done => {
    const subscription = fakeSubscription({
      reference_type: 'API_PRODUCT',
      reference_id: 'api-product-id',
      apiProduct: fakeApiProductSubscriptionDetails({ id: 'api-product-id' }),
    });

    service.get(subscription.id).subscribe(response => {
      expect(response.apiProduct).toEqual(subscription.apiProduct);
      done();
    });

    const req = httpTestingController.expectOne(
      `${TESTING_BASE_URL}/subscriptions/${subscription.id}?include=keys&include=consumerConfiguration&include=apiProduct`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(subscription);
  });

  it('should close subscription', done => {
    service.close('subscriptionId').subscribe(() => {
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions/subscriptionId/_close`);
    expect(req.request.method).toEqual('POST');

    req.flush(null);
  });
});
