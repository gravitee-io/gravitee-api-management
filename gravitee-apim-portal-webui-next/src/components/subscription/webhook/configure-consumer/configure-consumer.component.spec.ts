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

import { ConfigureConsumerComponent } from './configure-consumer.component';
import { Plan } from '../../../../entities/plan/plan';
import { fakePlan } from '../../../../entities/plan/plan.fixture';
import { fakeSubscription, Subscription } from '../../../../entities/subscription';
import { fakeSubscriptionConsumerConfiguration } from '../../../../entities/subscription/subscription-consumer-configuration.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';
import { ConsumerConfigurationComponent } from '../consumer-configuration';
import { ConfigureConsumerHarness } from './configure-consumer.harness';
import { SslTrustStoreHarness } from '../consumer-configuration-ssl/components/ssl-truststore/ssl-trust-store.harness';

const SUBSCRIPTION_ID = 'subscriptionId';

@Component({
  selector: 'app-test-component',
  template: ` <app-configure-consumer #consumerConfiguration [subscriptionId]="subscriptionId" />`,
  standalone: true,
  imports: [ConfigureConsumerComponent],
})
class TestComponent {
  @ViewChild('consumerConfiguration') consumerConfiguration!: ConsumerConfigurationComponent;
  subscriptionId = 'subscriptionId';
}

describe('ConfigureConsumerComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootHarnessLoader: HarnessLoader;
  let componentHarness: ConfigureConsumerHarness;

  const API_ID = 'testApiId';
  const PLAN_ID = 'plan-id';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent, AppTestingModule, ConsumerConfigurationComponent],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: { isFocusable: () => true, isTabbable: () => true },
      })
      .compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    componentHarness = await loader.getHarness(ConfigureConsumerHarness);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('ConfigureConsumerComponent', () => {
    const initComponent = (subscription: Subscription, plans?: Plan[]) => {
      expectSubscription(subscription);
      expectPlans(subscription, plans);
      fixture.detectChanges();
    };

    it('should create component with a basic configuration', async () => {
      const subscription = fakeSubscription({
        status: 'ACCEPTED',
        api: API_ID,
        plan: PLAN_ID,
        consumerConfiguration: fakeSubscriptionConsumerConfiguration(),
      });
      initComponent(subscription);

      const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();
      expect(await consumerConfigurationForm?.isSaveButtonDisabled()).toBeTruthy();
      expect(await consumerConfigurationForm?.isResetButtonDisabled()).toBeTruthy();

      await consumerConfigurationForm?.setInputTextValueFromControlName('channel', 'new-channel');

      expect(await consumerConfigurationForm?.isSaveButtonDisabled()).toBeFalsy();
      expect(await consumerConfigurationForm?.isResetButtonDisabled()).toBeFalsy();

      await save();

      expectSubscriptionUpdate();
      initComponent(subscription);
    });

    it('should reset the form', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription);

      const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();
      expect(await consumerConfigurationForm?.getInputTextFromControlName('channel')).toStrictEqual(consumerConfiguration.channel);
      expect(await consumerConfigurationForm?.getInputTextFromControlName('callbackUrl')).toStrictEqual(
        consumerConfiguration.entrypointConfiguration?.callbackUrl,
      );
      expect(await consumerConfigurationForm?.computeHeadersTableCells()).toEqual([
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

      await consumerConfigurationForm?.setInputTextValueFromControlName('channel', 'new-channel');
      await consumerConfigurationForm?.reset();

      expect(await consumerConfigurationForm?.getInputTextFromControlName('channel')).toStrictEqual(consumerConfiguration.channel);
    });

    it('should validate the form', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription);

      const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();

      expect(await consumerConfigurationForm?.isSaveButtonDisabled()).toBeTruthy();
      expect(await consumerConfigurationForm?.isResetButtonDisabled()).toBeTruthy();

      await consumerConfigurationForm?.setInputTextValueFromControlName('callbackUrl', 'not a url');
      expect(await consumerConfigurationForm?.getError()).toBeTruthy();
      expect(await consumerConfigurationForm?.isSaveButtonDisabled()).toBeTruthy();

      await consumerConfigurationForm?.setInputTextValueFromControlName('callbackUrl', 'https://dump.example.com');
      expect(await consumerConfigurationForm?.isSaveButtonDisabled()).toBeFalsy();

      await save();
      expectSubscriptionUpdate();
      initComponent(subscription);
    });

    it('should configure retry', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription);

      const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();

      expect(await consumerConfigurationForm?.getSelectedOption('retryOption')).toStrictEqual('No Retry');

      await consumerConfigurationForm?.selectOption('retryOption', 'Retry On Fail');
      await consumerConfigurationForm?.selectOption('retryStrategy', 'LINEAR');
      await expectRetryInputNumberError('maxAttempts', '0', 'Minimal value for maximum attempts is 1');
      await consumerConfigurationForm?.setInputTextValueFromControlName('maxAttempts', '1');
      await expectRetryInputNumberError('initialDelaySeconds', '0', 'Minimal value for initial delay seconds is 1');
      await consumerConfigurationForm?.setInputTextValueFromControlName('initialDelaySeconds', '2');
      await expectRetryInputNumberError('maxDelaySeconds', '0', 'Minimal value for maximum delay seconds is 1');
      await consumerConfigurationForm?.setInputTextValueFromControlName('maxDelaySeconds', '3');

      await save();
      expectSubscriptionUpdate();
      initComponent(subscription);

      expect(await consumerConfigurationForm?.getSelectedOption('retryOption')).toStrictEqual('Retry On Fail');
      expect(await consumerConfigurationForm?.getSelectedOption('retryStrategy')).toStrictEqual('LINEAR');
      expect(await consumerConfigurationForm?.getInputTextFromControlName('maxAttempts')).toStrictEqual('1');
      expect(await consumerConfigurationForm?.getInputTextFromControlName('initialDelaySeconds')).toStrictEqual('2');
      expect(await consumerConfigurationForm?.getInputTextFromControlName('maxDelaySeconds')).toStrictEqual('3');
    });

    it('reset changes', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription);

      const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();

      expect(await consumerConfigurationForm?.getSelectedOption('retryOption')).toStrictEqual('No Retry');

      await consumerConfigurationForm?.selectOption('retryOption', 'Retry On Fail');

      expect(await consumerConfigurationForm?.getSelectedOption('retryOption')).toStrictEqual('Retry On Fail');

      await consumerConfigurationForm?.reset();

      expect(await consumerConfigurationForm?.getSelectedOption('retryOption')).toStrictEqual('No Retry');
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

      const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();
      await consumerConfigurationForm?.toggle('#trustAll');
      fixture.detectChanges();

      expect(await loader.getHarnessOrNull(SslTrustStoreHarness)).toBeNull();
    });

    it('should add a comment to be able to save', async () => {
      const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
      const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
      initComponent(subscription, [fakePlan({ id: subscription.plan, comment_required: true })]);

      const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();

      await consumerConfigurationForm?.setInputTextValueFromControlName('callbackUrl', 'https://www.webhook-example.com/0987654321');

      await consumerConfigurationForm?.save();
      let reasonInput = await rootHarnessLoader.getHarness(MatInputHarness.with({ selector: `[formControlName="message"]` }));
      await reasonInput.setValue('');

      let okButton = await rootHarnessLoader.getHarness(MatButtonHarness.with({ selector: '#subscribeToApiCommentButton' }));
      await okButton.click();

      expectNoSubscriptionUpdate();

      await consumerConfigurationForm?.save();

      reasonInput = await rootHarnessLoader.getHarness(MatInputHarness.with({ selector: `[formControlName="message"]` }));
      await reasonInput.setValue('reason');

      okButton = await rootHarnessLoader.getHarness(MatButtonHarness.with({ selector: '#subscribeToApiCommentButton' }));
      await okButton.click();
      expectSubscriptionUpdate();
      initComponent(subscription, [fakePlan({ id: subscription.plan, comment_required: true })]);
    });

    describe('Authentication test', () => {
      it('should use token authentication', async () => {
        const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
        const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
        initComponent(subscription);

        const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();

        await consumerConfigurationForm?.selectOptionById('authType', 'Token security');
        await consumerConfigurationForm?.setInputTextValueFromControlName('token', 'awesome-token');
        expect(await consumerConfigurationForm?.isSaveButtonDisabled()).toBeFalsy();
        expect(await consumerConfigurationForm?.isResetButtonDisabled()).toBeFalsy();

        await consumerConfigurationForm?.setInputTextValueFromControlName('token', '');
        expect(await consumerConfigurationForm?.getError()).toStrictEqual('Token is required');

        await consumerConfigurationForm?.setInputTextValueFromControlName('token', 'awesome-token');
        await save();
        expectSubscriptionUpdate();
        initComponent(subscription);
      });

      it('should use basic authentication', async () => {
        const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
        const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
        initComponent(subscription);

        const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();

        await consumerConfigurationForm?.selectOptionById('authType', 'Basic security');
        await consumerConfigurationForm?.setInputTextValueFromControlName('username', 'user');
        await consumerConfigurationForm?.setInputTextValueFromControlName('password', 'pass');
        expect(await consumerConfigurationForm?.isSaveButtonDisabled()).toBeFalsy();
        expect(await consumerConfigurationForm?.isResetButtonDisabled()).toBeFalsy();

        await consumerConfigurationForm?.setInputTextValueFromControlName('username', '');
        expect(await consumerConfigurationForm?.getError()).toStrictEqual('Username is required');
        await consumerConfigurationForm?.setInputTextValueFromControlName('username', 'user');
        await consumerConfigurationForm?.setInputTextValueFromControlName('password', '');
        expect(await consumerConfigurationForm?.getError()).toStrictEqual('Password is required');
        await consumerConfigurationForm?.setInputTextValueFromControlName('password', 'pass');

        await save();
        expectSubscriptionUpdate();
        initComponent(subscription);
      });

      it('should use oauth2 authentication', async () => {
        const consumerConfiguration = fakeSubscriptionConsumerConfiguration();
        const subscription = fakeSubscription({ status: 'ACCEPTED', api: API_ID, plan: PLAN_ID, consumerConfiguration });
        initComponent(subscription);

        const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();

        await consumerConfigurationForm?.selectOptionById('authType', 'Oauth2 security');
        await consumerConfigurationForm?.setInputTextValueFromControlName('clientId', 'client-id');
        await consumerConfigurationForm?.setInputTextValueFromControlName('clientSecret', 'client-secret');
        await consumerConfigurationForm?.setInputTextValueFromControlName('endpoint', 'endpoint');
        expect(await consumerConfigurationForm?.isSaveButtonDisabled()).toBeFalsy();
        expect(await consumerConfigurationForm?.isResetButtonDisabled()).toBeFalsy();

        await consumerConfigurationForm?.setInputTextValueFromControlName('clientId', '');
        expect(await consumerConfigurationForm?.getError()).toStrictEqual('Client ID is required');
        await consumerConfigurationForm?.setInputTextValueFromControlName('clientId', 'client-id');
        await consumerConfigurationForm?.setInputTextValueFromControlName('clientSecret', '');
        expect(await consumerConfigurationForm?.getError()).toStrictEqual('Client secret is required');
        await consumerConfigurationForm?.setInputTextValueFromControlName('clientSecret', 'client-secret');
        await consumerConfigurationForm?.setInputTextValueFromControlName('endpoint', '');
        expect(await consumerConfigurationForm?.getError()).toStrictEqual('Endpoint is required');
        await consumerConfigurationForm?.setInputTextValueFromControlName('endpoint', 'endpoint');

        await save();
        expectSubscriptionUpdate();
        initComponent(subscription);
      });
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

  async function save() {
    const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();
    await consumerConfigurationForm?.save();
    const okButton = await rootHarnessLoader.getHarness(MatButtonHarness.with({ selector: '#subscribeToApiCommentButton' }));
    await okButton.click();
  }

  async function expectRetryInputNumberError(formControlName: string, value: string, error: string) {
    const consumerConfigurationForm = await componentHarness.getConsumerConfigurationComponentHarness();
    await consumerConfigurationForm?.setInputTextValueFromControlName(formControlName, value);
    expect(await consumerConfigurationForm?.getError()).toStrictEqual(error);
    await consumerConfigurationForm?.setInputTextValueFromControlName(formControlName, '');
    expect(await consumerConfigurationForm?.getError()).toStrictEqual(error);
  }

  function expectNoSubscriptionUpdate() {
    httpTestingController.expectNone({ method: 'PUT', url: `${TESTING_BASE_URL}/subscriptions/${SUBSCRIPTION_ID}` });
  }
});
