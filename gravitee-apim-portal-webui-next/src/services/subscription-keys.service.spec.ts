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

import { SubscriptionKeysService } from './subscription-keys.service';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('SubscriptionKeysService', () => {
  let service: SubscriptionKeysService;
  let httpTestingController: HttpTestingController;

  const subscriptionId = 'subscription-id';
  const apiKey = 'api-key';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(SubscriptionKeysService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should revoke key subscription', done => {
    service.revoke(subscriptionId, apiKey).subscribe(() => {
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions/${subscriptionId}/keys/${apiKey}/_revoke`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toBeNull();

    req.flush(null);
  });

  it('should renew key subscription', done => {
    service.renew(subscriptionId).subscribe(() => {
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions/${subscriptionId}/keys/_renew`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toBeNull();

    req.flush(null);
  });
});
