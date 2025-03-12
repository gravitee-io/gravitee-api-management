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
import { ActivatedRoute } from '@angular/router';

import { ConsumerConfigurationComponent } from './consumer-configuration.component';
import { ConsumerConfigurationComponentHarness } from './consumer-configuration.harness';
import { fakeSubscription, Subscription } from '../../../../entities/subscription';
import { fakeSubscriptionConsumerConfiguration } from '../../../../entities/subscription/subscription-consumer-configuration.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

@Component({
  selector: 'app-test-component',
  template: ` <app-consumer-configuration #consumerConfiguration></app-consumer-configuration>`,
  standalone: true,
  imports: [ConsumerConfigurationComponent],
})
class TestComponent {
  @ViewChild('consumerConfiguration') consumerConfiguration!: ConsumerConfigurationComponent;
}

describe('SubscriptionsDetailsComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let componentHarness: ConsumerConfigurationComponentHarness;

  const API_ID = 'testApiId';
  const SUBSCRIPTION_ID = 'subscriptionId';
  const PLAN_ID = 'plan-id';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { subscriptionId: SUBSCRIPTION_ID } } },
        },
      ],
    }).compileComponents();

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
    });
  });

  function expectSubscription(subscriptionResponse: Subscription = fakeSubscription()) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/subscriptions/${SUBSCRIPTION_ID}?include=keys&include=consumerConfiguration`)
      .flush(subscriptionResponse);
  }
});
