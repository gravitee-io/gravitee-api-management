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
import { Component, Input, output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { ApiProductSubscriptionDetailsComponent } from './api-product-subscription-details/api-product-subscription-details.component';
import SubscriptionDetailsComponent from './subscription-details.component';
import { SubscriptionDetailsHarness } from './subscription-details.harness';
import { fakeApiProductSubscriptionDetails, fakeSubscription } from '../../../entities/subscription';
import { Subscription } from '../../../entities/subscription/subscription';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { SubscriptionService } from '../../../services/subscription.service';
import { SubscriptionsDetailsComponent } from '../../api/api-details/api-tab-subscriptions/subscriptions-details/subscriptions-details.component';
import { subscriptionListBreadcrumb } from '../subscriptions/subscription-breadcrumbs';

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

@Component({
  selector: 'app-api-product-subscription-details',
  template: '',
  providers: [{ provide: ApiProductSubscriptionDetailsComponent, useExisting: MockApiProductSubscriptionDetailsComponent }],
})
class MockApiProductSubscriptionDetailsComponent {
  @Input() subscription!: Subscription;
  readonly apiKeyRenewed = output<void>();
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
        remove: { imports: [ApiProductSubscriptionDetailsComponent, SubscriptionsDetailsComponent] },
        add: { imports: [MockApiProductSubscriptionDetailsComponent, MockSubscriptionsDetailsComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(SubscriptionDetailsComponent);
    fixture.componentRef.setInput('subscriptionId', 'subscription-id');
  });

  it('should show subscriptions details when apiId is retrieved', async () => {
    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionDetailsHarness);
    expect(await harness.hasSubscriptionsDetails()).toBeTruthy();
  });

  it('should show API Product subscription details for an API Product reference', async () => {
    jest.mocked(subscriptionServiceMock.get!).mockReturnValue(
      of(
        fakeSubscription({
          api: undefined,
          reference_id: 'api-product-id',
          reference_type: 'API_PRODUCT',
          apiProduct: fakeApiProductSubscriptionDetails({ id: 'api-product-id' }),
        }),
      ),
    );

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionDetailsHarness);

    expect(await harness.hasApiProductSubscriptionDetails()).toBeTruthy();
    expect(await harness.hasSubscriptionsDetails()).toBeFalsy();
  });

  it('should reload API Product subscription details after API key renewal', async () => {
    jest.mocked(subscriptionServiceMock.get!).mockReturnValue(
      of(
        fakeSubscription({
          api: undefined,
          reference_id: 'api-product-id',
          reference_type: 'API_PRODUCT',
          apiProduct: fakeApiProductSubscriptionDetails({ id: 'api-product-id' }),
        }),
      ),
    );
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const apiProductDetails = fixture.debugElement.query(By.css('app-api-product-subscription-details'))
      .componentInstance as MockApiProductSubscriptionDetailsComponent;
    apiProductDetails.apiKeyRenewed.emit();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(subscriptionServiceMock.get).toHaveBeenCalledTimes(2);
  });

  it('should use the API reference ID when the legacy api field is absent', async () => {
    jest
      .mocked(subscriptionServiceMock.get!)
      .mockReturnValue(of(fakeSubscription({ api: undefined, reference_id: 'referenced-api-id', reference_type: 'API' })));

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionDetailsHarness);

    expect(await harness.hasSubscriptionsDetails()).toBeTruthy();
  });

  it('should show an error when the subscription cannot be loaded', async () => {
    jest.mocked(subscriptionServiceMock.get!).mockReturnValue(throwError(() => new Error('load error')));

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionDetailsHarness);

    expect(await harness.getErrorText()).toContain('An error occurred while loading the subscription');
  });

  it('should set breadcrumbs for subscription details', () => {
    const breadcrumbService = TestBed.inject(BreadcrumbService);
    fixture.detectChanges();
    expect(breadcrumbService.breadcrumbs()).toEqual([
      subscriptionListBreadcrumb(true),
      {
        id: 'subscription-subscription-id',
        label: $localize`:@@subscriptionTitle:Subscription ` + 'subscription-id',
      },
    ]);
  });
});
