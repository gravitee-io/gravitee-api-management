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
import { fakeHTTPProxyEndpoint } from '@gravitee/ui-policy-studio-angular/testing';

import { RuntimeAlertCreateService } from './runtime-alert-create.service';

import { Scope, Tuple } from '../../../../../entities/alert';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakeTenant } from '../../../../../entities/tenant/tenant.fixture';
import { fakePlan } from '../../../../../entities/plan/plan.fixture';
import { fakeSubscriptionPage } from '../../../../../entities/subscription/subscription.fixture';
import { fakePagedResult } from '../../../../../entities/pagedResult';
import { gatewayErrorKeys } from '../../../../../entities/gateway-error-keys/GatewayErrorKeys';
import { statusLoader } from '../../../../../entities/alerts/healthcheck.metrics';
import { fakeApiV2, fakeProxyApiV4 } from '../../../../../entities/management-api-v2';
import { fakeEndpointGroupV4 } from '../../../../../entities/management-api-v2/api/v4/endpointGroupV4.fixture';

describe('RuntimeAlertCreateService', () => {
  const API_ID = 'api-id';
  const APP_ID = 'app-id';
  let service: RuntimeAlertCreateService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<RuntimeAlertCreateService>(RuntimeAlertCreateService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should get tenants', done => {
    const tenant = fakeTenant();
    service.loadDataFromMetric('tenant', Scope.API, API_ID).subscribe(value => {
      expect(value).toStrictEqual([new Tuple(tenant.id, tenant.name)]);
      done();
    });

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`,
      method: 'GET',
    });

    req.flush([tenant]);
  });

  it('should get API plans', done => {
    const plan = fakePlan();
    service.loadDataFromMetric('plan', Scope.API, API_ID).subscribe(value => {
      expect(value).toStrictEqual([new Tuple(plan.id, plan.name)]);
      done();
    });

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans?status=PUBLISHED`,
      method: 'GET',
    });

    req.flush([plan]);
  });

  it('should get APPLICATION subscriptions', done => {
    const plan = fakePlan({ id: APP_ID });
    service.loadDataFromMetric('plan', Scope.APPLICATION, APP_ID).subscribe(value => {
      expect(value).toStrictEqual([new Tuple(plan.id, plan.name)]);
      done();
    });

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APP_ID}/subscriptions?expand=security`,
      method: 'GET',
    });

    const metadata = {};
    metadata[plan.id] = { name: plan.name };
    const response = fakePagedResult([fakeSubscriptionPage({ plan: plan.id })], null, metadata);
    req.flush(response);
  });

  it('should get error keys', () => {
    service.loadDataFromMetric('error.key', Scope.API, API_ID).subscribe(value => {
      expect(value).toHaveLength(gatewayErrorKeys.length);
    });
  });

  it.each(['status.old', 'status.new'])('should get status keys', key => {
    service.loadDataFromMetric(key, Scope.API, API_ID).subscribe(value => {
      expect(value).toHaveLength(statusLoader().length);
    });
  });

  it('should get API V2 ENDPOINTS names', done => {
    const endpoint1 = { name: 'endpoint-1', type: 'type' };
    const endpoint2 = { name: 'endpoint-2', type: 'type' };
    const endpoint3 = { name: 'endpoint-3', type: 'type' };
    const api = fakeApiV2({
      id: API_ID,
      proxy: {
        groups: [
          { name: 'groupe-1', endpoints: [endpoint1, endpoint2] },
          { name: 'groupe-2', endpoints: [endpoint3] },
        ],
      },
    });

    service.loadDataFromMetric('endpoint.name', Scope.API, API_ID).subscribe(value => {
      expect(value).toStrictEqual([
        new Tuple(endpoint1.name, endpoint1.name),
        new Tuple(endpoint2.name, endpoint2.name),
        new Tuple(endpoint3.name, endpoint3.name),
      ]);
      done();
    });

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
        method: 'GET',
      })
      .flush(api);
  });

  it('should get API V4 ENDPOINTS names', done => {
    const endpoint1 = fakeHTTPProxyEndpoint({ name: 'endpoint-1' });
    const endpoint2 = fakeHTTPProxyEndpoint({ name: 'endpoint-2' });
    const endpoint3 = fakeHTTPProxyEndpoint({ name: 'endpoint-3' });
    const api = fakeProxyApiV4({
      id: API_ID,
      endpointGroups: [fakeEndpointGroupV4({ endpoints: [endpoint1, endpoint2] }), fakeEndpointGroupV4({ endpoints: [endpoint3] })],
    });

    service.loadDataFromMetric('endpoint.name', Scope.API, API_ID).subscribe(value => {
      expect(value).toStrictEqual([
        new Tuple(endpoint1.name, endpoint1.name),
        new Tuple(endpoint2.name, endpoint2.name),
        new Tuple(endpoint3.name, endpoint3.name),
      ]);
      done();
    });

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
        method: 'GET',
      })
      .flush(api);
  });
});
