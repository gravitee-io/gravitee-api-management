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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ApiProductSubscriptionDetailsComponent } from './api-product-subscription-details.component';
import { ApiProductSubscriptionDetailsHarness } from './api-product-subscription-details.harness';
import { fakeApplication } from '../../../../entities/application/application.fixture';
import { fakeApiProductSubscriptionDetails, fakeSubscription } from '../../../../entities/subscription';
import { ApplicationService } from '../../../../services/application.service';
import { AppTestingModule } from '../../../../testing/app-testing.module';

describe('ApiProductSubscriptionDetailsComponent', () => {
  let fixture: ComponentFixture<ApiProductSubscriptionDetailsComponent>;
  let applicationService: jest.Mocked<Pick<ApplicationService, 'get'>>;

  beforeEach(async () => {
    applicationService = {
      get: jest.fn().mockReturnValue(of(fakeApplication({ name: 'My application' }))),
    };

    await TestBed.configureTestingModule({
      imports: [ApiProductSubscriptionDetailsComponent, AppTestingModule],
      providers: [provideNoopAnimations(), provideRouter([]), { provide: ApplicationService, useValue: applicationService }],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductSubscriptionDetailsComponent);
  });

  it('should show API Product subscription summary and included APIs', async () => {
    fixture.componentRef.setInput(
      'subscription',
      fakeSubscription({
        status: 'ACCEPTED',
        start_at: '2026-08-01T10:00:00Z',
        end_at: '2027-08-01T10:00:00Z',
        apiProduct: fakeApiProductSubscriptionDetails({
          plan: { id: 'plan-id', name: 'Gold', security: 'KEY_LESS', mode: 'STANDARD' },
        }),
      }),
    );

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductSubscriptionDetailsHarness);

    expect(await harness.getSummaryText()).toContain('Commerce Product');
    expect(await harness.getSummaryText()).toContain('Accepted');
    expect(await harness.getSummaryText()).toContain('My application');
    expect(await harness.getCredentialsText()).toContain('No credentials are required');
    expect(await harness.getApiCount()).toBe(1);
    expect(await harness.getApplicationLink()).toContain('/dashboard/applications/');
  });

  it('should show the subscription lifecycle state instead of credentials', async () => {
    fixture.componentRef.setInput(
      'subscription',
      fakeSubscription({
        status: 'PENDING',
        apiProduct: fakeApiProductSubscriptionDetails(),
      }),
    );

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductSubscriptionDetailsHarness);

    expect(await harness.getCredentialsText()).toContain('subscription request is being reviewed');
    expect(fixture.nativeElement.querySelectorAll('app-api-product-subscription-api-access app-copy-code')).toHaveLength(0);
  });

  it.each(['CLOSED', 'REJECTED'] as const)('should not show KEY_LESS access for a %s subscription', async status => {
    fixture.componentRef.setInput(
      'subscription',
      fakeSubscription({
        status,
        apiProduct: fakeApiProductSubscriptionDetails({
          plan: { id: 'plan-id', name: 'Keyless', security: 'KEY_LESS', mode: 'STANDARD' },
        }),
      }),
    );

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductSubscriptionDetailsHarness);

    expect(await harness.getCredentialsText()).toContain(
      status === 'CLOSED' ? 'subscription is closed' : 'subscription request was rejected',
    );
    expect(fixture.nativeElement.textContent).toContain('API access is not available for the current subscription status');
    expect(fixture.nativeElement.querySelectorAll('app-api-product-subscription-api-access app-copy-code')).toHaveLength(0);
  });

  it('should show OAuth2 application credentials once', async () => {
    fixture.componentRef.setInput(
      'subscription',
      fakeSubscription({
        status: 'ACCEPTED',
        apiProduct: fakeApiProductSubscriptionDetails({
          plan: { id: 'plan-id', name: 'OAuth', security: 'OAUTH2', mode: 'STANDARD' },
        }),
      }),
    );

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductSubscriptionDetailsHarness);

    expect(await harness.getCredentialsText()).toContain('Client ID');
    expect(await harness.getCredentialsText()).toContain('Client Secret');
    expect(fixture.nativeElement.querySelectorAll('[data-testid="product-subscription-credentials"] app-copy-code')).toHaveLength(2);
  });
});
