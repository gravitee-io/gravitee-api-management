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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { ConsumerConfigurationComponent } from './consumer-configuration.component';
import { ConsumerConfigurationComponentHarness } from './consumer-configuration.harness';
import { Plan } from '../../../../entities/plan/plan';
import { fakePlan } from '../../../../entities/plan/plan.fixture';
import { fakeSubscription, Subscription } from '../../../../entities/subscription';
import { fakeSubscriptionConsumerConfiguration } from '../../../../entities/subscription/subscription-consumer-configuration.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';
import { SslTrustStoreHarness } from '../consumer-configuration-ssl/components/ssl-truststore/ssl-trust-store.harness';

const SUBSCRIPTION_ID = 'subscriptionId';

@Component({
  selector: 'app-test-component',
  template: ` <app-consumer-configuration #consumerConfiguration [subscriptionId]="subscriptionId"></app-consumer-configuration>`,
  standalone: true,
  imports: [ConsumerConfigurationComponent],
})
class TestComponent {
  @ViewChild('consumerConfiguration') consumerConfiguration!: ConsumerConfigurationComponent;
  subscriptionId = SUBSCRIPTION_ID;
}

describe('SubscriptionsDetailsComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootHarnessLoader: HarnessLoader;
  let componentHarness: ConsumerConfigurationComponentHarness;

  const API_ID = 'testApiId';
  const PLAN_ID = 'plan-id';

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TestComponent, AppTestingModule] })
      .overrideProvider(InteractivityChecker, {
        useValue: { isFocusable: () => true, isTabbable: () => true },
      })
      .compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    componentHarness = await loader.getHarness(ConsumerConfigurationComponentHarness);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('consumerConfiguration tests', () => {
    const initComponent = (subscription: Subscription, plans?: Plan[]) => {
      expectSubscription(subscription);
      expectPlans(subscription, plans);
      fixture.detectChanges();
    };

    it('should create component with a basic configuration', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription);

      expect(await componentHarness.getInputTextFromControlName('channel')).toStrictEqual(consumerConfiguration.channel);
      expect(await componentHarness.getInputTextFromControlName('callbackUrl')).toStrictEqual(
        consumerConfiguration.entrypointConfiguration?.callbackUrl,
      );
      expect(await componentHarness.computeHeadersTableCells()).toEqual([
        {
          name: 'Content-Type',
          value: 'application/json',
        },
        {
          name: 'X-Custom-Key',
          value: '1234',
        },
        {
          name: '',
          value: '',
        },
      ]);

      expect(await componentHarness.isSaveButtonDisabled()).toBeTruthy();
      expect(await componentHarness.isResetButtonDisabled()).toBeTruthy();

      await componentHarness.setInputTextValueFromControlName('channel', 'new-channel');

      expect(await componentHarness.isSaveButtonDisabled()).toBeFalsy();
      expect(await componentHarness.isResetButtonDisabled()).toBeFalsy();

      await save();
      expectSubscriptionUpdate();
    });

    it('should reset the form', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription);

      expect(await componentHarness.getInputTextFromControlName('channel')).toStrictEqual(consumerConfiguration.channel);
      expect(await componentHarness.getInputTextFromControlName('callbackUrl')).toStrictEqual(
        consumerConfiguration.entrypointConfiguration?.callbackUrl,
      );
      expect(await componentHarness.computeHeadersTableCells()).toEqual([
        {
          name: 'Content-Type',
          value: 'application/json',
        },
        {
          name: 'X-Custom-Key',
          value: '1234',
        },
        {
          name: '',
          value: '',
        },
      ]);

      await componentHarness.setInputTextValueFromControlName('channel', 'new-channel');
      await componentHarness.reset();

      expect(await componentHarness.getInputTextFromControlName('channel')).toStrictEqual(consumerConfiguration.channel);
    });

    it('should validate the form', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription);

      expect(await componentHarness.isSaveButtonDisabled()).toBeTruthy();
      expect(await componentHarness.isResetButtonDisabled()).toBeTruthy();

      await componentHarness.setInputTextValueFromControlName('callbackUrl', 'not a url');
      expect(await componentHarness.getError()).toBeTruthy();
      expect(await componentHarness.isSaveButtonDisabled()).toBeTruthy();

      await componentHarness.setInputTextValueFromControlName('callbackUrl', 'https://dump.example.com');
      expect(await componentHarness.isSaveButtonDisabled()).toBeFalsy();

      await save();
      expectSubscriptionUpdate();
    });

    it('should configure retry', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription);

      await componentHarness.selectOption('retryOption', 'Retry On Fail');
      await componentHarness.selectOption('retryStrategy', 'LINEAR');
      await expectRetryInputNumberError('maxAttempts', '0', 'Minimal value for maximum attempts is 1');
      await componentHarness.setInputTextValueFromControlName('maxAttempts', '1');
      await expectRetryInputNumberError('initialDelaySeconds', '0', 'Minimal value for initial delay seconds is 1');
      await componentHarness.setInputTextValueFromControlName('initialDelaySeconds', '2');
      await expectRetryInputNumberError('maxDelaySeconds', '0', 'Minimal value for maximum delay seconds is 1');
      await componentHarness.setInputTextValueFromControlName('maxDelaySeconds', '3');

      await save();
      expectSubscriptionUpdate();

      await componentHarness.selectOption('retryOption', 'No Retry');
      expect(await componentHarness.getSelectedOption('retryOption')).toStrictEqual('No Retry');

      await componentHarness.reset();

      expect(await componentHarness.getSelectedOption('retryOption')).toStrictEqual('Retry On Fail');
      expect(await componentHarness.getSelectedOption('retryStrategy')).toStrictEqual('LINEAR');
      expect(await componentHarness.getInputTextFromControlName('maxAttempts')).toStrictEqual('1');
      expect(await componentHarness.getInputTextFromControlName('initialDelaySeconds')).toStrictEqual('2');
      expect(await componentHarness.getInputTextFromControlName('maxDelaySeconds')).toStrictEqual('3');
    });

    it('should hide trust store configuration when trust all is enabled', async () => {
      const baseConfiguration = fakeSubscriptionConsumerConfiguration();
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration({
        ...baseConfiguration,
        entrypointConfiguration: {
          ...baseConfiguration.entrypointConfiguration,
          ssl: { ...baseConfiguration.entrypointConfiguration.ssl, trustAll: false },
        },
      });
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription);

      expect(await loader.getHarnessOrNull(SslTrustStoreHarness)).toBeTruthy();

      await componentHarness.toggle('#trustAll');
      fixture.detectChanges();

      expect(await loader.getHarnessOrNull(SslTrustStoreHarness)).toBeNull();
    });

    it('should add a comment to be able to save', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription, [fakePlan({ id: subscription.plan, comment_required: true })]);

      await componentHarness.setInputTextValueFromControlName('callbackUrl', 'https://www.webhook-example.com/0987654321');

      await componentHarness.save();
      let reasonInput = await rootHarnessLoader.getHarness(MatInputHarness.with({ selector: `[formControlName="message"]` }));
      await reasonInput.setValue('');

      let okButton = await rootHarnessLoader.getHarness(MatButtonHarness.with({ selector: '#subscribeToApiCommentButton' }));
      await okButton.click();

      expectNoSubscriptionUpdate();

      await componentHarness.save();
      reasonInput = await rootHarnessLoader.getHarness(MatInputHarness.with({ selector: `[formControlName="message"]` }));
      await reasonInput.setValue('reason');

      okButton = await rootHarnessLoader.getHarness(MatButtonHarness.with({ selector: '#subscribeToApiCommentButton' }));
      await okButton.click();
      expectSubscriptionUpdate();
    });
  });

  function expectSubscription(subscriptionResponse: Subscription = fakeSubscription()) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/subscriptions/${SUBSCRIPTION_ID}?include=keys&include=consumerConfiguration`)
      .flush(subscriptionResponse);
  }

  function expectPlans(subscription: Subscription, plans: Plan[] = [fakePlan({ id: subscription.plan })]) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/testApiId/plans?size=-1`).flush({ data: plans });
  }

  function expectSubscriptionUpdate(subscriptionResponse: Subscription = fakeSubscription()) {
    httpTestingController
      .expectOne({ method: 'PUT', url: `${TESTING_BASE_URL}/subscriptions/${SUBSCRIPTION_ID}` })
      .flush(subscriptionResponse);
  }

  function expectNoSubscriptionUpdate() {
    httpTestingController.expectNone({ method: 'PUT', url: `${TESTING_BASE_URL}/subscriptions/${SUBSCRIPTION_ID}` });
  }

  async function save() {
    await componentHarness.save();
    const okButton = await rootHarnessLoader.getHarness(MatButtonHarness.with({ selector: '#subscribeToApiCommentButton' }));
    await okButton.click();
  }

  async function expectRetryInputNumberError(formControlName: string, value: string, error: string) {
    await componentHarness.setInputTextValueFromControlName(formControlName, value);
    expect(await componentHarness.getError()).toStrictEqual(error);
    await componentHarness.setInputTextValueFromControlName(formControlName, '');
    expect(await componentHarness.getError()).toStrictEqual(error);
  }
});
