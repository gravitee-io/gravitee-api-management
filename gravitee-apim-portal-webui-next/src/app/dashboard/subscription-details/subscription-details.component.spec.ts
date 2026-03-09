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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient } from '@angular/common/http';
import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import SubscriptionDetailsComponent from './subscription-details.component';
import { SubscriptionDetailsHarness } from './subscription-details.harness';
import { Subscription } from '../../../entities/subscription/subscription';
import { SubscriptionService } from '../../../services/subscription.service';
import { SubscriptionsDetailsComponent } from '../../api/api-details/api-tab-subscriptions/subscriptions-details/subscriptions-details.component';

@Component({
  selector: 'app-subscriptions-details',
  standalone: true,
  template: '',
  providers: [{ provide: SubscriptionsDetailsComponent, useExisting: MockSubscriptionsDetailsComponent }],
})
class MockSubscriptionsDetailsComponent {
  @Input() apiId!: string;
  @Input() subscriptionId!: string;
}

describe('SubscriptionDetailsComponent', () => {
  let fixture: ComponentFixture<SubscriptionDetailsComponent>;
  let subscriptionServiceMock: Partial<SubscriptionService>;

  beforeEach(async () => {
    subscriptionServiceMock = {
      get: jest.fn().mockReturnValue(of({ api: 'my-api-id' } as Subscription)),
    };

    await TestBed.configureTestingModule({
      imports: [SubscriptionDetailsComponent],
      providers: [{ provide: SubscriptionService, useValue: subscriptionServiceMock }, provideHttpClient()],
    })
      .overrideComponent(SubscriptionDetailsComponent, {
        remove: { imports: [SubscriptionsDetailsComponent] },
        add: { imports: [MockSubscriptionsDetailsComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(SubscriptionDetailsComponent);
    fixture.componentRef.setInput('subscriptionId', 'subscription-id');
  });

  it('should show subscriptions details when apiId is retrieved', async () => {
    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionDetailsHarness);
    expect(await harness.hasSubscriptionsDetails()).toBeTruthy();
  });
});
