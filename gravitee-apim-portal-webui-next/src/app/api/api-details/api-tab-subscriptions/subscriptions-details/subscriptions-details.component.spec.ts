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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SubscriptionsDetailsComponent } from './subscriptions-details.component';
import { ApiAccessHarness } from '../../../../../components/api-access/api-access.harness';
import { Api } from '../../../../../entities/api/api';
import { fakeApi } from '../../../../../entities/api/api.fixtures';
import { Application } from '../../../../../entities/application/application';
import { fakeApplication } from '../../../../../entities/application/application.fixture';
import { Configuration } from '../../../../../entities/configuration/configuration';
import { fakePlan, fakePlansResponse } from '../../../../../entities/plan/plan.fixture';
import { PlansResponse } from '../../../../../entities/plan/plans-response';
import { Subscription } from '../../../../../entities/subscription/subscription';
import { fakeSubscription, fakeSubscriptionResponse } from '../../../../../entities/subscription/subscription.fixture';
import { SubscriptionsResponse } from '../../../../../entities/subscription/subscriptions-response';
import { ConfigService } from '../../../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../../testing/app-testing.module';

describe('SubscriptionsDetailsComponent', () => {
  let fixture: ComponentFixture<SubscriptionsDetailsComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  const API_ID = 'testApiId';

  @Injectable()
  class CustomConfigurationServiceStub {
    get baseURL(): string {
      return TESTING_BASE_URL;
    }
    get configuration(): Configuration {
      return {
        portal: {
          apikeyHeader: 'X-My-Apikey',
        },
      };
    }
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionsDetailsComponent, AppTestingModule],
      providers: [
        {
          provide: ConfigService,
          useClass: CustomConfigurationServiceStub,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionsDetailsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.componentInstance.subscriptionId = 'testSubscriptionId';
    fixture.componentInstance.apiId = 'testApiId';

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('subscription pending', () => {
    beforeEach(() => {
      expectSubscriptionWithKeys(fakeSubscription({ status: 'PENDING' }));
      expectSubscriptionList(fakeSubscriptionResponse(), API_ID);
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__pending')).toBeDefined();
    });
  });

  describe('subscription rejected', () => {
    beforeEach(() => {
      expectSubscriptionWithKeys(fakeSubscription({ status: 'REJECTED' }));
      expectSubscriptionList(fakeSubscriptionResponse(), API_ID);
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__rejected')).toBeDefined();
    });
  });

  describe('subscription closed', () => {
    beforeEach(() => {
      expectSubscriptionWithKeys(fakeSubscription({ status: 'CLOSED' }));
      expectSubscriptionList(fakeSubscriptionResponse(), API_ID);
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__closed')).toBeDefined();
    });
  });

  describe('subscription paused', () => {
    beforeEach(() => {
      expectSubscriptionWithKeys(fakeSubscription({ status: 'PAUSED' }));
      expectSubscriptionList(fakeSubscriptionResponse(), API_ID);
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__paused')).toBeDefined();
    });
  });

  describe('subscription rejected', () => {
    beforeEach(() => {
      expectSubscriptionWithKeys(fakeSubscription({ status: 'REJECTED' }));
      expectSubscriptionList(fakeSubscriptionResponse(), API_ID);
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__rejected')).toBeDefined();
    });
  });

  describe('subscription accepted', () => {
    const PLAN_ID = 'plan-id';
    const APP_ID = 'app-id';
    const APP_NAME = 'app-name';
    const API_KEY = 'my-api-key';

    beforeEach(() => {
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID });
      const subscriptionWithKeys = { ...subscription, keys: [{ key: API_KEY, id: '1', application: { id: APP_ID, name: APP_NAME } }] };
      expectSubscriptionWithKeys(subscriptionWithKeys);
      expectSubscriptionList(
        fakeSubscriptionResponse({
          data: [subscription],
          metadata: {
            [`${API_ID}`]: {
              entrypoints: [
                {
                  target: 'https://gw/entrypoint',
                },
              ],
            },
          },
        }),
        API_ID,
      );
      expectApplicationsList(fakeApplication({ id: APP_ID, name: APP_NAME }));
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should show subscription details with Api Key', async () => {
      expectPlansList(fakePlansResponse({ data: [fakePlan({ id: PLAN_ID, security: 'API_KEY' })] }));
      fixture.detectChanges();

      const apiAccess = await harnessLoader.getHarness(ApiAccessHarness);
      expect(await apiAccess.getApiKey()).toStrictEqual(API_KEY);
      expect(await apiAccess.getBaseURL()).toStrictEqual('https://gw/entrypoint');
      expect(await apiAccess.getCommandLine()).toStrictEqual(`curl --header "X-My-Apikey: ${API_KEY}" https://gw/entrypoint`);
    });

    it('should show subscription details with Oauth2', async () => {
      expectPlansList(fakePlansResponse({ data: [fakePlan({ id: PLAN_ID, security: 'OAUTH2' })] }));
      fixture.detectChanges();

      const apiAccess = await harnessLoader.getHarness(ApiAccessHarness);
      expect(await apiAccess.getClientId()).toStrictEqual('zLgNDMUCbbCBDNnpBGb-WOV_lNrUlQlAlUiSditR9Es');
      expect(await apiAccess.getClientSecret()).toContain('***');
      await apiAccess.toggleClientSecretVisibility();
      expect(await apiAccess.getClientSecret()).toStrictEqual('3zEYOXPqqCyaq7os--Nf1-6jrHjL0AjumFz4CL78nwQ');
    });
  });

  function expectSubscriptionWithKeys(subscriptionResponse: Subscription = fakeSubscription()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions/testSubscriptionId?include=keys`).flush(subscriptionResponse);
  }

  function expectSubscriptionList(subscriptionResponse: SubscriptionsResponse = fakeSubscriptionResponse(), apiId: string) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions?apiId=${apiId}`).flush(subscriptionResponse);
  }

  function expectPlansList(plansResponse: PlansResponse = fakePlansResponse()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/testApiId/plans?size=-1`).flush(plansResponse);
  }

  function expectApplicationsList(applicationsResponse: Application = fakeApplication()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/99c6cbe6-eead-414d-86cb-e6eeadc14db3`).flush(applicationsResponse);
  }
  function expectGetApi(api: Api = fakeApi()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/${api.id}`).flush(fakeApi());
  }
});
