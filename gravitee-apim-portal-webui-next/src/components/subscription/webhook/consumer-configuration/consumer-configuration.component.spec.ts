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
import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { ConsumerConfigurationComponent } from './consumer-configuration.component';
import { ConsumerConfigurationComponentHarness } from './consumer-configuration.harness';
import { fakeSubscription, Subscription } from '../../../../entities/subscription';
import { fakeSubscriptionConsumerConfiguration } from '../../../../entities/subscription/subscription-consumer-configuration.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

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
  let componentHarness: ConsumerConfigurationComponentHarness;

  const API_ID = 'testApiId';
  const PLAN_ID = 'plan-id';

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TestComponent, AppTestingModule] }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    componentHarness = await loader.getHarness(ConsumerConfigurationComponentHarness);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('consumerConfiguration tests', () => {
    const initComponent = (subscription: Subscription) => {
      expectSubscription(subscription);
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

      await componentHarness.save();
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

      await componentHarness.save();
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

      await componentHarness.save();
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

      expect(await loader.getHarnessOrNull(MatSelectHarness.with({ selector: '[formControlName="type"]' }))).toBeTruthy();

      await componentHarness.toggle('#trustAll');

      expect(await loader.getHarnessOrNull(MatSelectHarness.with({ selector: '[formControlName="type"]' }))).toBeNull();
    });
  });

  function expectSubscription(subscriptionResponse: Subscription = fakeSubscription()) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/subscriptions/${SUBSCRIPTION_ID}?include=keys&include=consumerConfiguration`)
      .flush(subscriptionResponse);
  }

  function expectSubscriptionUpdate(subscriptionResponse: Subscription = fakeSubscription()) {
    httpTestingController
      .expectOne({ method: 'PUT', url: `${TESTING_BASE_URL}/subscriptions/${SUBSCRIPTION_ID}` })
      .flush(subscriptionResponse);
  }

  async function expectRetryInputNumberError(formControlName: string, value: string, error: string) {
    await componentHarness.setInputTextValueFromControlName(formControlName, value);
    expect(await componentHarness.getError()).toStrictEqual(error);
    await componentHarness.setInputTextValueFromControlName(formControlName, '');
    expect(await componentHarness.getError()).toStrictEqual(error);
  }
});
