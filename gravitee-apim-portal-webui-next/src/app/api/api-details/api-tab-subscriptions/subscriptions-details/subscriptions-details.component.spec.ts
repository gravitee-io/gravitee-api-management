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
import { MatButtonHarness } from '@angular/material/button/testing';

import { SubscriptionsDetailsComponent } from './subscriptions-details.component';
import { ApiAccessHarness } from '../../../../../components/api-access/api-access.harness';
import { SubscriptionInfoHarness } from '../../../../../components/subscription-info/subscription-info.harness';
import { Api } from '../../../../../entities/api/api';
import { fakeApi } from '../../../../../entities/api/api.fixtures';
import { Application } from '../../../../../entities/application/application';
import { fakeApplication } from '../../../../../entities/application/application.fixture';
import { Configuration } from '../../../../../entities/configuration/configuration';
import { fakeUserApiPermissions } from '../../../../../entities/permission/permission.fixtures';
import { fakePlan, fakePlansResponse } from '../../../../../entities/plan/plan.fixture';
import { PlansResponse } from '../../../../../entities/plan/plans-response';
import {
  fakeSubscription,
  fakeSubscriptionResponse,
  Subscription,
  SubscriptionConsumerStatusEnum,
  SubscriptionsResponse,
  SubscriptionStatusEnum,
} from '../../../../../entities/subscription';
import { fakeSubscriptionConsumerConfiguration } from '../../../../../entities/subscription/subscription-consumer-configuration.fixture';
import { ConfigService } from '../../../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../../testing/app-testing.module';
import { CloseSubscriptionDialogComponent } from '../../../../dashboard/subscription-details/close-subscription-dialog/close-subscription-dialog.component';
import { CloseSubscriptionDialogHarness } from '../../../../dashboard/subscription-details/close-subscription-dialog/close-subscription-dialog.harness';

describe('SubscriptionsDetailsComponent', () => {
  let fixture: ComponentFixture<SubscriptionsDetailsComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;
  let rootLoader: HarnessLoader;

  const API_ID = 'testApiId';
  const CONFIGURATION_KAFKA_SASL_MECHANISMS = ['PLAIN', 'SCRAM-SHA-256', 'SCRAM-SHA-512'];

  @Injectable()
  class CustomConfigurationServiceStub {
    get baseURL(): string {
      return TESTING_BASE_URL;
    }
    get configuration(): Configuration {
      return {
        portal: {
          apikeyHeader: 'X-My-Apikey',
          kafkaSaslMechanisms: CONFIGURATION_KAFKA_SASL_MECHANISMS,
        },
      };
    }
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionsDetailsComponent, CloseSubscriptionDialogComponent, AppTestingModule],
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
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

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
      expectGetApiPermissions();
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__pending')).toBeDefined();
    });
  });

  describe('subscription closed', () => {
    beforeEach(() => {
      expectSubscriptionWithKeys(fakeSubscription({ status: 'CLOSED' }));
      expectGetApiPermissions();
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
      expectGetApiPermissions();
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
      expectGetApiPermissions();
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
      expectGetApiPermissions();
      expectApplicationsList(fakeApplication({ id: APP_ID, name: APP_NAME }));
      expectGetApi(fakeApi({ id: API_ID, entrypoints: ['https://gw/entrypoint'] }));
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

  describe('with push plan', () => {
    const PLAN_ID = 'plan-id';
    const APP_ID = 'app-id';
    const APP_NAME = 'app-name';

    it.each`
      status        | expected
      ${'ACCEPTED'} | ${true}
      ${'PENDING'}  | ${true}
      ${'PAUSED'}   | ${true}
      ${'REJECTED'} | ${false}
      ${'CLOSED'}   | ${false}
    `('should show subscription configuration edition button according to plan', async ({ status, expected }) => {
      const subscription = fakeSubscription({
        api: API_ID,
        plan: PLAN_ID,
        status: status as SubscriptionStatusEnum,
        consumerConfiguration: fakeSubscriptionConsumerConfiguration(),
      });
      expectSubscriptionWithKeys(subscription);
      expectGetApiPermissions();
      expectApplicationsList(fakeApplication({ id: APP_ID, name: APP_NAME }));
      expectGetApi(fakeApi({ id: API_ID, entrypoints: ['https://gw/entrypoint'] }));
      expectPlansList(fakePlansResponse({ data: [fakePlan({ id: PLAN_ID, mode: 'PUSH', security: 'KEY_LESS' })] }));
      fixture.detectChanges();

      const button = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.configuration__header__button' }));
      expect(!!button).toBe(expected);
    });
  });

  describe('subscription accepted for Native API', () => {
    const PLAN_ID = 'plan-id';
    const APP_ID = 'app-id';
    const APP_NAME = 'app-name';
    const API_KEY = 'my-api-key';
    const API_KEY_HASH = 'api-key-hash';

    beforeEach(() => {
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID });
      const subscriptionWithKeys = {
        ...subscription,
        keys: [{ key: API_KEY, id: '1', hash: API_KEY_HASH, application: { id: APP_ID, name: APP_NAME } }],
      };
      expectSubscriptionWithKeys(subscriptionWithKeys);
      expectGetApiPermissions();
      expectApplicationsList(fakeApplication({ id: APP_ID, name: APP_NAME }));
      expectGetApi(fakeApi({ id: API_ID, entrypoints: ['https://gw/entrypoint'], type: 'NATIVE' }));
      expectPlansList(fakePlansResponse({ data: [fakePlan({ id: PLAN_ID, security: 'API_KEY' })] }));
      fixture.detectChanges();
    });

    it('should show API Key hash as username', async () => {
      const apiAccess = await harnessLoader.getHarness(ApiAccessHarness);
      expect(await apiAccess.getPlainConfig()).toContain(`username="${API_KEY_HASH}"`);
      expect(await apiAccess.getPlainConfig()).toContain(`password="${API_KEY}"`);
    });
  });

  describe('When user does not have API Plan READ permission', () => {
    it('should still show API Key plan information', async () => {
      const PLAN_ID = 'plan-id';
      const APP_ID = 'app-id';
      const APP_NAME = 'app-name';
      const API_KEY = 'my-api-key';

      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID });
      const subscriptionWithKeys = { ...subscription, keys: [{ key: API_KEY, id: '1', application: { id: APP_ID, name: APP_NAME } }] };
      expectSubscriptionWithKeys(subscriptionWithKeys);
      expectGetApiPermissions(fakeUserApiPermissions({ PLAN: [] }));
      expectSubscriptionList(fakeSubscriptionResponse({ metadata: { [PLAN_ID]: { securityType: 'API_KEY' } } }), API_ID);
      expectApplicationsList(fakeApplication({ id: APP_ID, name: APP_NAME }));
      expectGetApi(fakeApi({ id: API_ID, entrypoints: ['https://gw/entrypoint'] }));
      fixture.detectChanges();

      const apiAccess = await harnessLoader.getHarness(ApiAccessHarness);
      expect(await apiAccess.getApiKey()).toStrictEqual(API_KEY);
      expect(await apiAccess.getBaseURL()).toStrictEqual('https://gw/entrypoint');
      expect(await apiAccess.getCommandLine()).toStrictEqual(`curl --header "X-My-Apikey: ${API_KEY}" https://gw/entrypoint`);
    });
  });

  describe('consumer status started', () => {
    beforeEach(() => {
      expectSubscriptionWithKeys(fakeSubscription({ consumerStatus: SubscriptionConsumerStatusEnum.STARTED }));
      expectGetApiPermissions();
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should hide retry subscription button', async () => {
      const button = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.retry-subscription-button' }));
      expect(button).toBeNull();
    });
  });

  describe('retry-subscription-button click', () => {
    beforeEach(() => {
      expectSubscriptionWithKeys(fakeSubscription({ consumerStatus: SubscriptionConsumerStatusEnum.FAILURE }));
      expectGetApiPermissions();
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    afterEach(() => {
      expectPostChangeConsumerStatus();
      expectSubscriptionWithKeys(fakeSubscription());
      expectGetApiPermissions();
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should call correct endpoints', async () => {
      const button = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.retry-subscription-button' }));

      expect(button).not.toBeNull();

      await button?.click();
    });
  });

  describe('close-subscription-button click', () => {
    const url = `${TESTING_BASE_URL}/subscriptions/testSubscriptionId/_close`;
    let subscriptionInfoHarness: SubscriptionInfoHarness;

    beforeEach(async () => {
      expectSubscriptionWithKeys(fakeSubscription({ status: 'ACCEPTED' }));
      expectGetApiPermissions();
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
      subscriptionInfoHarness = await harnessLoader.getHarness(SubscriptionInfoHarness);
    });

    it('should close subscription when popup is confirmed', async () => {
      const button = await subscriptionInfoHarness.getCloseButton();
      expect(button).not.toBeNull();
      await button!.click();

      const confirmDialog = await closeConfirmDialog();
      expect(confirmDialog).not.toBeNull();
      await confirmDialog!.confirm();

      httpTestingController.expectOne(url).flush(null);

      expectSubscriptionWithKeys(fakeSubscription({ status: 'CLOSED' }));
      expectGetApiPermissions();
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should not close subscription when popup is denied', async () => {
      const button = await subscriptionInfoHarness.getCloseButton();
      expect(button).not.toBeNull();
      await button?.click();

      const confirmDialog = await closeConfirmDialog();
      expect(confirmDialog).not.toBeNull();
      await confirmDialog!.cancel();

      httpTestingController.expectNone(url);
    });

    async function closeConfirmDialog(): Promise<CloseSubscriptionDialogHarness | null> {
      return await rootLoader.getHarnessOrNull(CloseSubscriptionDialogHarness);
    }
  });

  describe('consumer status stopped', () => {
    beforeEach(() => {
      expectSubscriptionWithKeys(fakeSubscription({ consumerStatus: SubscriptionConsumerStatusEnum.STOPPED }));
      expectGetApiPermissions();
      expectPlansList(fakePlansResponse());
      expectApplicationsList(fakeApplication());
      expectGetApi(fakeApi({ id: API_ID }));
    });

    it('should hide retry subscription button', async () => {
      const button = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.retry-subscription-button' }));
      expect(button).toBeNull();
    });
  });

  function expectSubscriptionWithKeys(subscriptionResponse: Subscription = fakeSubscription()) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/subscriptions/testSubscriptionId?include=keys&include=consumerConfiguration`)
      .flush(subscriptionResponse);
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
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/${api.id}`).flush(api);
  }

  function expectGetApiPermissions(permissions = fakeUserApiPermissions({ PLAN: ['R'] })) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/permissions?apiId=${API_ID}`).flush(permissions);
  }

  function expectPostChangeConsumerStatus() {
    const url = `${TESTING_BASE_URL}/subscriptions/testSubscriptionId/_resumeFailure`;
    httpTestingController.expectOne(url).flush(fakeSubscription());
  }
});
