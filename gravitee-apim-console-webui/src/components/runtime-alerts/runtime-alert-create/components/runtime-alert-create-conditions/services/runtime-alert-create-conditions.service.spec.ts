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

import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { RuntimeAlertCreateConditionsService } from './runtime-alert-create-conditions.service';

import { Scope, Tuple } from '../../../../../../entities/alert';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { fakeTenant } from '../../../../../../entities/tenant/tenant.fixture';
import { fakePlan } from '../../../../../../entities/plan/plan.fixture';
import { fakeApplicationSubscription } from '../../../../../../entities/subscription/subscription.fixture';
import { fakePagedResult } from '../../../../../../entities/pagedResult';
import { gatewayErrorKeys } from '../../../../../../entities/gateway-error-keys/GatewayErrorKeys';

describe('RuntimeAlertCreateConditionsService', () => {
  const API_ID = 'api-id';
  const APP_ID = 'app-id';
  let service: RuntimeAlertCreateConditionsService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<RuntimeAlertCreateConditionsService>(RuntimeAlertCreateConditionsService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should get tenants', (done) => {
    const tenant = fakeTenant();
    service.loadDataFromMetric('tenant', Scope.API, API_ID).subscribe((value) => {
      expect(value).toStrictEqual([new Tuple(tenant.id, tenant.name)]);
      done();
    });

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`,
      method: 'GET',
    });

    req.flush([tenant]);
  });

  it('should get API plans', (done) => {
    const plan = fakePlan();
    service.loadDataFromMetric('plan', Scope.API, API_ID).subscribe((value) => {
      expect(value).toStrictEqual([new Tuple(plan.id, plan.name)]);
      done();
    });

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans?status=PUBLISHED`,
      method: 'GET',
    });

    req.flush([plan]);
  });

  it('should get APPLICATION subscriptions', (done) => {
    const plan = fakePlan({ id: APP_ID });
    service.loadDataFromMetric('plan', Scope.APPLICATION, APP_ID).subscribe((value) => {
      expect(value).toStrictEqual([new Tuple(plan.id, plan.name)]);
      done();
    });

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APP_ID}/subscriptions?expand=security`,
      method: 'GET',
    });

    const metadata = {};
    metadata[plan.id] = { name: plan.name };
    const response = fakePagedResult([fakeApplicationSubscription({ plan: plan.id })], null, metadata);
    req.flush(response);
  });

  it('should get error keys', () => {
    service.loadDataFromMetric('error.key', Scope.API, API_ID).subscribe((value) => {
      expect(value).toHaveLength(gatewayErrorKeys.length);
    });
  });
});
