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

import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApplicationSubscriptionListModule } from './application-subscription-list.module';
import { ApplicationSubscriptionListComponent } from './application-subscription-list.component';
import { ApplicationSubscriptionListHarness } from './application-subscription-list.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakeApplication } from '../../../../../entities/application/Application.fixture';
import { SubscriptionPage } from '../../../../../entities/subscription/subscription';
import { fakePagedResult } from '../../../../../entities/pagedResult';
import { Api, fakeProxyApiV4 } from '../../../../../entities/management-api-v2';
import { fakeSubscriptionPage } from '../../../../../entities/subscription/subscription.fixture';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { Application } from '../../../../../entities/application/Application';

@Component({
  template: ` <application-subscription-list #subscriptionListComponent></application-subscription-list> `,
  standalone: false,
})
class TestComponent {
  @ViewChild('subscriptionListComponent') subscriptionListComponent: ApplicationSubscriptionListComponent;
}

describe('ApplicationSubscriptionListComponent', () => {
  const APPLICATION_ID = 'app-id';
  const APP = fakeApplication({ id: APPLICATION_ID });
  const API_ID = 'api-id';
  const PLAN_ID = 'plan-id';
  const DEFAULT_PERMISSIONS = ['application-subscription-c', 'application-subscription-r'];

  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [ApplicationSubscriptionListModule, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
    }).compileComponents();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('URL query params tests', () => {
    beforeEach(async () => {
      await init();
    });

    it('should display default filters', fakeAsync(async () => {
      await initComponent([]);
      const harness = await loader.getHarness(ApplicationSubscriptionListHarness);

      const selectedApi = await harness.getApis();
      expect(selectedApi).toHaveLength(0);

      const statusSelect = await harness.getStatusSelect();
      expect(await statusSelect.isDisabled()).toEqual(false);
      expect(await statusSelect.getValueText()).toEqual('Accepted, Paused, Pending');

      const apiKeyInput = await harness.getApiKeyInput();
      expect(await apiKeyInput.isDisabled()).toEqual(false);
      expect(await apiKeyInput.getValue()).toEqual('');
    }));

    it('should init filters from params', fakeAsync(async () => {
      await initComponent([], {
        apis: API_ID,
        status: 'CLOSED,REJECTED',
        apiKey: 'api-key-1',
      });
      const harness = await loader.getHarness(ApplicationSubscriptionListHarness);

      const selectedApis = await harness.getApis();
      expect(selectedApis).toHaveLength(1);
      expect(selectedApis).toEqual([`${API_ID} (PO)`]);

      const statusSelect = await harness.getStatusSelect();
      expect(await statusSelect.isDisabled()).toEqual(false);
      expect(await statusSelect.getValueText()).toEqual('Closed, Rejected');

      const apiKeyInput = await harness.getApiKeyInput();
      expect(await apiKeyInput.isDisabled()).toEqual(false);
      expect(await apiKeyInput.getValue()).toEqual('api-key-1');
    }));

    it('should reset filters from params', fakeAsync(async () => {
      await initComponent([], {
        apis: API_ID,
        status: 'CLOSED,REJECTED',
        apiKey: 'api-key-1',
      });
      const harness = await loader.getHarness(ApplicationSubscriptionListHarness);

      const resetFilterButton = await harness.getResetFilterButton();
      await resetFilterButton.click();
      tick(800);
      expectSubscriptionsGetRequest([]);

      const selectedApis = await harness.getApis();
      expect(selectedApis).toHaveLength(0);

      const statusSelect = await harness.getStatusSelect();
      expect(await statusSelect.isDisabled()).toEqual(false);
      expect(await statusSelect.getValueText()).toEqual('Accepted, Paused, Pending');

      const apiKeyInput = await harness.getApiKeyInput();
      expect(await apiKeyInput.isDisabled()).toEqual(false);
      expect(await apiKeyInput.getValue()).toEqual('');
    }));
  });

  describe('subscriptionsTable tests', () => {
    beforeEach(async () => {
      await init();
    });

    it('should display an empty table', fakeAsync(async () => {
      await initComponent([]);
      const harness = await loader.getHarness(ApplicationSubscriptionListHarness);
      const { headerCells, rowCells } = await harness.computeSubscriptionsTableCells();
      expect(headerCells).toEqual([
        {
          plan: 'Plan',
          api: 'API',
          createdAt: 'Created at',
          processedAt: 'Processed at',
          securityType: 'Security type',
          startingAt: 'Started at',
          endAt: 'Ended at',
          status: 'Status',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([['There is no subscription (yet).']]);
    }));

    it('should display a table with one row', fakeAsync(async () => {
      const subscription = fakeSubscriptionPage({ api: API_ID, application: APPLICATION_ID, plan: PLAN_ID });
      await initComponent([subscription]);

      const harness = await loader.getHarness(ApplicationSubscriptionListHarness);
      const { headerCells, rowCells } = await harness.computeSubscriptionsTableCells();
      expect(headerCells).toEqual([
        {
          plan: 'Plan',
          api: 'API',
          createdAt: 'Created at',
          processedAt: 'Processed at',
          securityType: 'Security type',
          startingAt: 'Started at',
          endAt: 'Ended at',
          status: 'Status',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([
        ['API_KEY', 'Plan Name', 'Api Name - 1', expect.any(String), expect.any(String), expect.any(String), '', 'Accepted', ''],
      ]);
      expectApiGetRequest(fakeProxyApiV4({ id: subscription.api, name: 'api', primaryOwner: { displayName: 'PO' } }));
    }));

    it('should search closed subscription', fakeAsync(async () => {
      await initComponent([]);

      const harness = await loader.getHarness(ApplicationSubscriptionListHarness);
      await harness.selectStatus('Closed');
      tick(400);

      const subscription = fakeSubscriptionPage({
        api: API_ID,
        application: APPLICATION_ID,
        plan: PLAN_ID,
        status: 'CLOSED',
      });
      expectSubscriptionsGetRequest([subscription], ['ACCEPTED', 'CLOSED', 'PAUSED', 'PENDING']);
      expectApiGetRequest(fakeProxyApiV4({ id: subscription.api, name: 'api', primaryOwner: { displayName: 'PO' } }));
    }));

    it('should search with a specific api key', fakeAsync(async () => {
      const key = 'custom-api-key';
      await initComponent([]);

      const harness = await loader.getHarness(ApplicationSubscriptionListHarness);
      await harness.addApiKey(key);
      tick(400);

      const subscription = fakeSubscriptionPage({
        api: API_ID,
        application: APPLICATION_ID,
        plan: PLAN_ID,
      });
      expectSubscriptionsGetRequest([subscription], ['ACCEPTED', 'PAUSED', 'PENDING'], null, key);
      expectApiGetRequest(fakeProxyApiV4({ id: subscription.api, name: 'api', primaryOwner: { displayName: 'PO' } }));
    }));

    it('should search with a specific api', fakeAsync(async () => {
      const apiName = 'api';
      const api = fakeProxyApiV4({ id: API_ID, name: apiName });
      await initComponent([]);

      const harness = await loader.getHarness(ApplicationSubscriptionListHarness);
      await harness.searchApi(apiName);
      expectSubscribedApis([{ id: api.id, name: api.name }]);

      await harness.selectApi(apiName);
      expectApiGetRequest(api);

      expect(await harness.getApiTags()).toEqual([apiName]);

      await harness.selectStatus('Paused');
      tick(400);

      const subscription = fakeSubscriptionPage({
        api: api.id,
        application: APPLICATION_ID,
        plan: PLAN_ID,
      });
      expectSubscriptionsGetRequest([subscription], ['ACCEPTED', 'PENDING'], [api.id]);
      expectApiGetRequest(fakeProxyApiV4({ id: subscription.api, name: api.name, primaryOwner: { displayName: 'PO' } }));
    }));
  });

  async function initComponent(
    subscriptions: SubscriptionPage[],
    params?: { apis?: string; status?: string; apiKey?: string },
    permissions = DEFAULT_PERMISSIONS,
  ) {
    await TestBed.overrideProvider(ActivatedRoute, {
      useValue: {
        snapshot: {
          params: { applicationId: APP.id },
          queryParams: { ...(params ? params : {}) },
        },
      },
    }).compileComponents();
    if (permissions.length > 0) {
      await TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: permissions }).compileComponents();
    }

    fixture = TestBed.createComponent(TestComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();

    tick(800);
    if (params?.apis) {
      params.apis
        .split(',')
        .forEach((id) => expectApiGetRequest(fakeProxyApiV4({ id, name: API_ID, primaryOwner: { displayName: 'PO' } })));
    }

    expectSubscriptionsGetRequest(subscriptions, params?.status?.split(','), params?.apis?.split(','), params?.apiKey);
    expectApplicationGetRequest(APP);
    subscriptions.forEach((subscription) => {
      expectApiGetRequest(fakeProxyApiV4({ id: subscription.api, name: 'api', primaryOwner: { displayName: 'PO' } }));
    });
  }

  const expectSubscriptionsGetRequest = (subscriptions: SubscriptionPage[] = [], status?: string[], apis?: string[], apiKey?: string) => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/subscriptions?page=1&size=10${
          status ? `&status=${status.join(',')}` : '&status=ACCEPTED,PAUSED,PENDING'
        }${apis ? `&api=${apis.join(',')}` : ''}${apiKey ? `&api_key=${apiKey}` : ''}`,
        method: 'GET',
      })
      .flush(
        fakePagedResult(subscriptions, undefined, {
          ...Object.fromEntries(
            subscriptions?.map((subscription) => [
              subscription.plan,
              {
                name: 'Plan Name',
                securityType: 'API_KEY',
              },
            ]),
          ),
          ...Object.fromEntries(
            subscriptions?.map((subscription) => [
              subscription.api,
              {
                name: 'Api Name',
                apiVersion: '1',
              },
            ]),
          ),
        }),
      );
    fixture.detectChanges();
  };

  const expectApiGetRequest = (api: Api) => {
    const requests = httpTestingController.match({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      method: 'GET',
    });
    requests.map((request) => {
      if (!request.cancelled) request.flush(api);
    });
  };

  const expectSubscribedApis = (apis = []) => {
    const requests = httpTestingController.match({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/subscribed`,
      method: 'GET',
    });
    requests.map((request) => {
      if (!request.cancelled) request.flush(apis);
    });
  };

  const expectApplicationGetRequest = (application: Application): void => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${application.id}`,
        method: 'GET',
      })
      .flush(application);
  };
});
