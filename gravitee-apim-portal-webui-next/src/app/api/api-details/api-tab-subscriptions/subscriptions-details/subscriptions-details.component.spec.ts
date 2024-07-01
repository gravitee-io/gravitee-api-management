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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { SubscriptionsDetailsComponent } from './subscriptions-details.component';
import { fakeApi, fakeApisResponse } from '../../../../../entities/api/api.fixtures';
import { ApisResponse } from '../../../../../entities/api/apis-response';
import { Application } from '../../../../../entities/application/application';
import { fakeApplication } from '../../../../../entities/application/application.fixture';
import { Subscription, SubscriptionData } from '../../../../../entities/subscription/subscription';
import { fakeSubscription, fakeSubscriptionResponse } from '../../../../../entities/subscription/subscription.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../../testing/app-testing.module';

describe('SubscriptionsDetailsComponent', () => {
  let fixture: ComponentFixture<SubscriptionsDetailsComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionsDetailsComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionsDetailsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.componentInstance.subscriptionApplicationId = 'testApplicationId';
    fixture.componentInstance.apiId = 'testApiId';

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('subscription pending', () => {
    beforeEach(() => {
      expectSubscriptionListWithKeys(fakeSubscription({ status: 'PENDING' }));
      expectSubscriptionList(fakeSubscriptionResponse(), 'testApiId');
      expectApiPlansList(fakeApisResponse());
      expectApplicationsList(fakeApplication());
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__pending')).toBeDefined();
    });
  });

  describe('subscription rejected', () => {
    beforeEach(() => {
      expectSubscriptionListWithKeys(fakeSubscription({ status: 'REJECTED' }));
      expectSubscriptionList(fakeSubscriptionResponse(), 'testApiId');
      expectApiPlansList(fakeApisResponse());
      expectApplicationsList(fakeApplication());
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__rejected')).toBeDefined();
    });
  });
  describe('subscription closed', () => {
    beforeEach(() => {
      expectSubscriptionListWithKeys(fakeSubscription({ status: 'CLOSED' }));
      expectSubscriptionList(fakeSubscriptionResponse(), 'testApiId');
      expectApiPlansList(fakeApisResponse());
      expectApplicationsList(fakeApplication());
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__closed')).toBeDefined();
    });
  });

  describe('subscription paused', () => {
    beforeEach(() => {
      expectSubscriptionListWithKeys(fakeSubscription({ status: 'PAUSED' }));
      expectSubscriptionList(fakeSubscriptionResponse(), 'testApiId');
      expectApiPlansList(fakeApisResponse());
      expectApplicationsList(fakeApplication());
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__paused')).toBeDefined();
    });
  });

  describe('subscription rejected', () => {
    beforeEach(() => {
      expectSubscriptionListWithKeys(fakeSubscription({ status: 'REJECTED' }));
      expectSubscriptionList(fakeSubscriptionResponse(), 'testApiId');
      expectApiPlansList(fakeApisResponse());
      expectApplicationsList(fakeApplication());
    });

    it('should show pending status ', async () => {
      expect(fixture.nativeElement.querySelector('.subscriptions-details__rejected')).toBeDefined();
    });
  });

  describe('subscription accepted', () => {
    it('should show subscription details with Api Key', async () => {
      expectSubscriptionListWithKeys(fakeSubscription({ status: 'ACCEPTED', api: 'c42f51dd-fa20-4e68-af51-ddfa20be682c' }));
      expectSubscriptionList(fakeSubscriptionResponse(), 'testApiId');
      expectApplicationsList(fakeApplication());
      expectApiPlansList(
        fakeApisResponse({
          data: [fakeApi({ security: 'API_KEY' })],
        }),
      );
      const apiKeyInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[aria-label="API key input"]' }));
      expect(await apiKeyInput.getValue()).toStrictEqual('240760d9-7a50-4e7c-8406-657cdee57fde');
    });

    it('should show subscription details with Oauth2', async () => {
      expectSubscriptionListWithKeys(fakeSubscription({ status: 'ACCEPTED', api: 'c42f51dd-fa20-4e68-af51-ddfa20be682c' }));
      expectSubscriptionList(fakeSubscriptionResponse(), 'testApiId');
      expectApplicationsList(fakeApplication());
      expectApiPlansList(
        fakeApisResponse({
          data: [fakeApi({ security: 'OAUTH2' })],
        }),
      );
      const apiKeyInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[aria-label="Client ID"]' }));
      expect(await apiKeyInput.getValue()).toStrictEqual('zLgNDMUCbbCBDNnpBGb-WOV_lNrUlQlAlUiSditR9Es');
    });
  });

  function expectSubscriptionListWithKeys(subscriptionResponse: SubscriptionData = fakeSubscription()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions/testApplicationId?include=keys`).flush(subscriptionResponse);
  }

  function expectSubscriptionList(subscriptionResponse: Subscription = fakeSubscriptionResponse(), apiId: string) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions?apiId=${apiId}`).flush(subscriptionResponse);
  }

  function expectApiPlansList(apisResponse: ApisResponse = fakeApisResponse()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/testApiId/plans?size=-1`).flush(apisResponse);
  }

  function expectApplicationsList(applicationsResponse: Application = fakeApplication()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/99c6cbe6-eead-414d-86cb-e6eeadc14db3`).flush(applicationsResponse);
  }
});
