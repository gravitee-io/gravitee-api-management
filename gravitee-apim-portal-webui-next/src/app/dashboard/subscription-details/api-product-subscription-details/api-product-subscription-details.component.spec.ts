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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { ApiProductSubscriptionDetailsComponent } from './api-product-subscription-details.component';
import { ApiProductSubscriptionDetailsHarness } from './api-product-subscription-details.harness';
import { ConfirmDialogHarness } from '../../../../components/confirm-dialog/confirm-dialog.harness';
import { fakeApplication } from '../../../../entities/application/application.fixture';
import { UserApplicationPermissions } from '../../../../entities/permission/permission';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import {
  fakeApiProductSubscriptionDetails,
  fakeSubscription,
  Subscription,
  SubscriptionConsumerStatusEnum,
} from '../../../../entities/subscription';
import { ApplicationService } from '../../../../services/application.service';
import { PermissionsService } from '../../../../services/permissions.service';
import { SubscriptionService } from '../../../../services/subscription.service';
import { AppTestingModule } from '../../../../testing/app-testing.module';

describe('ApiProductSubscriptionDetailsComponent', () => {
  let fixture: ComponentFixture<ApiProductSubscriptionDetailsComponent>;
  let rootLoader: HarnessLoader;
  let applicationService: jest.Mocked<Pick<ApplicationService, 'get'>>;
  let permissionsService: jest.Mocked<Pick<PermissionsService, 'getApplicationPermissions'>>;
  let subscriptionService: jest.Mocked<Pick<SubscriptionService, 'changeConsumerStatus' | 'close' | 'get' | 'resumeConsumerStatus'>>;

  const acceptedSubscription = (modifier: Partial<Subscription> = {}): Subscription =>
    fakeSubscription({
      status: 'ACCEPTED',
      consumerStatus: SubscriptionConsumerStatusEnum.STARTED,
      reference_type: 'API_PRODUCT',
      reference_id: 'api-product-id',
      api: undefined,
      apiProduct: fakeApiProductSubscriptionDetails(),
      ...modifier,
    });

  beforeEach(async () => {
    applicationService = {
      get: jest.fn().mockReturnValue(of(fakeApplication({ name: 'My application' }))),
    };
    permissionsService = {
      getApplicationPermissions: jest.fn(),
    };
    subscriptionService = {
      changeConsumerStatus: jest.fn(),
      close: jest.fn(),
      get: jest.fn(),
      resumeConsumerStatus: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ApiProductSubscriptionDetailsComponent, AppTestingModule],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ApplicationService, useValue: applicationService },
        { provide: PermissionsService, useValue: permissionsService },
        { provide: SubscriptionService, useValue: subscriptionService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductSubscriptionDetailsComponent);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  });

  async function init(
    subscription: Subscription = acceptedSubscription(),
    permissions: UserApplicationPermissions = fakeUserApplicationPermissions({ SUBSCRIPTION: ['U', 'D'] }),
  ): Promise<ApiProductSubscriptionDetailsHarness> {
    permissionsService.getApplicationPermissions.mockReturnValue(of(permissions));
    subscriptionService.get.mockReturnValue(of(subscription));
    subscriptionService.changeConsumerStatus.mockReturnValue(of(subscription));
    subscriptionService.close.mockReturnValue(of(undefined));
    subscriptionService.resumeConsumerStatus.mockReturnValue(of(subscription));
    fixture.componentRef.setInput('subscription', subscription);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductSubscriptionDetailsHarness);
  }

  it('should show API Product subscription summary and included APIs', async () => {
    const harness = await init(
      acceptedSubscription({
        start_at: '2026-08-01T10:00:00Z',
        end_at: '2027-08-01T10:00:00Z',
        apiProduct: fakeApiProductSubscriptionDetails({
          plan: { id: 'plan-id', name: 'Gold', security: 'KEY_LESS', mode: 'STANDARD' },
        }),
      }),
    );

    expect(await harness.getSummaryText()).toContain('Commerce Product');
    expect(await harness.getSummaryText()).toContain('Accepted');
    expect(await harness.getSummaryText()).toContain('Active');
    expect(await harness.getSummaryText()).toContain('My application');
    expect(await harness.getCredentialsText()).toContain('No credentials are required');
    expect(await harness.getApiCount()).toBe(1);
    expect(await harness.getApplicationLink()).toContain('/dashboard/applications/');
  });

  it('should show the subscription lifecycle state instead of credentials', async () => {
    const harness = await init(acceptedSubscription({ status: 'PENDING' }));

    expect(await harness.getCredentialsText()).toContain('subscription request is being reviewed');
    expect(fixture.nativeElement.querySelectorAll('app-api-product-subscription-api-access app-copy-code')).toHaveLength(0);
  });

  it.each(['CLOSED', 'REJECTED'] as const)('should not show KEY_LESS access for a %s subscription', async status => {
    const harness = await init(
      acceptedSubscription({
        status,
        apiProduct: fakeApiProductSubscriptionDetails({
          plan: { id: 'plan-id', name: 'Keyless', security: 'KEY_LESS', mode: 'STANDARD' },
        }),
      }),
    );

    expect(await harness.getCredentialsText()).toContain(
      status === 'CLOSED' ? 'subscription is closed' : 'subscription request was rejected',
    );
    expect(fixture.nativeElement.textContent).toContain('API access is not available for the current subscription status');
    expect(fixture.nativeElement.querySelectorAll('app-api-product-subscription-api-access app-copy-code')).toHaveLength(0);
    expect(await harness.getPauseButton()).toBeNull();
    expect(await harness.getResumeButton()).toBeNull();
    expect(await harness.getCloseButton()).toBeNull();
  });

  it('should show OAuth2 application credentials once', async () => {
    const harness = await init(
      acceptedSubscription({
        apiProduct: fakeApiProductSubscriptionDetails({
          plan: { id: 'plan-id', name: 'OAuth', security: 'OAUTH2', mode: 'STANDARD' },
        }),
      }),
    );

    expect(await harness.getCredentialsText()).toContain('Client ID');
    expect(await harness.getCredentialsText()).toContain('Client Secret');
    expect(fixture.nativeElement.querySelectorAll('[data-testid="product-subscription-credentials"] app-copy-code')).toHaveLength(2);
  });

  it('should distinguish a publisher-paused subscription from a consumer pause', async () => {
    const harness = await init(acceptedSubscription({ status: 'PAUSED', consumerStatus: SubscriptionConsumerStatusEnum.STOPPED }));

    expect(await harness.getSummaryText()).toContain('Unavailable');
    expect(await harness.getCredentialsText()).toContain('paused by the API publisher');
    expect(await harness.getCredentialsText()).not.toContain('API access for this subscription is paused');
  });

  it('should show a failed consumer without exposing credentials', async () => {
    const harness = await init(
      acceptedSubscription({
        consumerStatus: SubscriptionConsumerStatusEnum.FAILURE,
        failureCause: 'Connection refused',
      }),
    );

    expect(await harness.getSummaryText()).toContain('Unavailable');
    expect(await harness.getCredentialsText()).toContain('Subscription consumer failed');
    expect(await harness.getCredentialsText()).toContain('Connection refused');
    expect(fixture.nativeElement.querySelectorAll('[data-testid="product-subscription-credentials"] app-api-keys-list')).toHaveLength(0);
    expect(fixture.nativeElement.querySelectorAll('[data-testid="product-subscription-credentials"] app-copy-code')).toHaveLength(0);
    expect(await harness.getPauseButton()).toBeNull();
    expect(await harness.getResumeButton()).toBeNull();
    expect(await harness.getRetryButton()).not.toBeNull();
    expect(await harness.getCloseButton()).not.toBeNull();
  });

  it('should show Pause and Close actions for an active accepted subscription', async () => {
    const harness = await init();

    expect(await harness.getPauseButton()).not.toBeNull();
    expect(await harness.getResumeButton()).toBeNull();
    expect(await harness.getCloseButton()).not.toBeNull();
  });

  it('should hide lifecycle actions without application permissions', async () => {
    const harness = await init(acceptedSubscription(), fakeUserApplicationPermissions());

    expect(await harness.getPauseButton()).toBeNull();
    expect(await harness.getResumeButton()).toBeNull();
    expect(await harness.getCloseButton()).toBeNull();
  });

  it('should hide Retry without the application update permission', async () => {
    const failedSubscription = acceptedSubscription({ consumerStatus: SubscriptionConsumerStatusEnum.FAILURE });
    const harness = await init(failedSubscription, fakeUserApplicationPermissions({ SUBSCRIPTION: ['D'] }));

    expect(await harness.getRetryButton()).toBeNull();
    expect(await harness.getCloseButton()).not.toBeNull();
  });

  it('should pause the subscription, reload details and disable API access', async () => {
    const initialSubscription = acceptedSubscription();
    const pausedSubscription = acceptedSubscription({ consumerStatus: SubscriptionConsumerStatusEnum.STOPPED });
    const harness = await init(initialSubscription);
    subscriptionService.changeConsumerStatus.mockReturnValue(of(pausedSubscription));
    subscriptionService.get.mockReturnValue(of(pausedSubscription));

    await (await harness.getPauseButton())!.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(subscriptionService.changeConsumerStatus).toHaveBeenCalledWith(initialSubscription.id, SubscriptionConsumerStatusEnum.STOPPED);
    expect(subscriptionService.get).toHaveBeenCalledWith(initialSubscription.id);
    expect(await harness.getFeedbackText()).toContain('paused');
    expect(await harness.getFeedbackAttribute('role')).toEqual('status');
    expect(await harness.getFeedbackAttribute('aria-live')).toEqual('polite');
    expect(await harness.getResumeButton()).not.toBeNull();
    expect(await harness.getCredentialsText()).toContain('subscription is paused');
    expect(fixture.nativeElement.textContent).toContain('API access is not available for the current subscription status');
  });

  it('should disable Pause and Close while pausing is in progress', async () => {
    const action$ = new Subject<Subscription>();
    const harness = await init();
    subscriptionService.changeConsumerStatus.mockReturnValue(action$);

    await (await harness.getPauseButton())!.click();
    fixture.detectChanges();

    expect(await (await harness.getPauseButton())!.isDisabled()).toBe(true);
    expect(await (await harness.getCloseButton())!.isDisabled()).toBe(true);

    action$.error(new Error('Request failed'));
  });

  it('should resume a consumer-paused subscription', async () => {
    const pausedSubscription = acceptedSubscription({ consumerStatus: SubscriptionConsumerStatusEnum.STOPPED });
    const resumedSubscription = acceptedSubscription();
    const harness = await init(pausedSubscription);
    subscriptionService.changeConsumerStatus.mockReturnValue(of(resumedSubscription));
    subscriptionService.get.mockReturnValue(of(resumedSubscription));

    await (await harness.getResumeButton())!.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(subscriptionService.changeConsumerStatus).toHaveBeenCalledWith(pausedSubscription.id, SubscriptionConsumerStatusEnum.STARTED);
    expect(await harness.getFeedbackText()).toContain('resumed');
    expect(await harness.getPauseButton()).not.toBeNull();
  });

  it('should disable Resume and Close while resuming is in progress', async () => {
    const action$ = new Subject<Subscription>();
    const pausedSubscription = acceptedSubscription({ consumerStatus: SubscriptionConsumerStatusEnum.STOPPED });
    const harness = await init(pausedSubscription);
    subscriptionService.changeConsumerStatus.mockReturnValue(action$);

    await (await harness.getResumeButton())!.click();
    fixture.detectChanges();

    expect(await (await harness.getResumeButton())!.isDisabled()).toBe(true);
    expect(await (await harness.getCloseButton())!.isDisabled()).toBe(true);

    action$.error(new Error('Request failed'));
  });

  it('should retry a failed consumer and reload subscription details', async () => {
    const failedSubscription = acceptedSubscription({
      consumerStatus: SubscriptionConsumerStatusEnum.FAILURE,
      failureCause: 'Connection refused',
    });
    const resumedSubscription = acceptedSubscription();
    const harness = await init(failedSubscription);
    subscriptionService.resumeConsumerStatus.mockReturnValue(of(resumedSubscription));
    subscriptionService.get.mockReturnValue(of(resumedSubscription));

    await (await harness.getRetryButton())!.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(subscriptionService.resumeConsumerStatus).toHaveBeenCalledWith(failedSubscription.id);
    expect(subscriptionService.get).toHaveBeenCalledWith(failedSubscription.id);
    expect(await harness.getFeedbackText()).toContain('retried');
    expect(await harness.getRetryButton()).toBeNull();
    expect(await harness.getPauseButton()).not.toBeNull();
  });

  it('should disable Retry and Close while retrying is in progress', async () => {
    const action$ = new Subject<Subscription>();
    const failedSubscription = acceptedSubscription({ consumerStatus: SubscriptionConsumerStatusEnum.FAILURE });
    const harness = await init(failedSubscription);
    subscriptionService.resumeConsumerStatus.mockReturnValue(action$);

    await (await harness.getRetryButton())!.click();
    fixture.detectChanges();

    expect(await (await harness.getRetryButton())!.isDisabled()).toBe(true);
    expect(await (await harness.getCloseButton())!.isDisabled()).toBe(true);

    action$.error(new Error('Request failed'));
    fixture.detectChanges();

    expect(await harness.getFeedbackText()).toContain('could not be retried');
    expect(await (await harness.getRetryButton())!.isDisabled()).toBe(false);
  });

  it('should close the subscription after confirmation', async () => {
    const initialSubscription = acceptedSubscription();
    const closedSubscription = acceptedSubscription({ status: 'CLOSED' });
    const harness = await init(initialSubscription);
    subscriptionService.get.mockReturnValue(of(closedSubscription));

    await (await harness.getCloseButton())!.click();
    const dialog = await rootLoader.getHarness(ConfirmDialogHarness);
    await dialog.confirm();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(subscriptionService.close).toHaveBeenCalledWith(initialSubscription.id);
    expect(await harness.getFeedbackText()).toContain('closed');
    expect(await harness.getPauseButton()).toBeNull();
    expect(await harness.getResumeButton()).toBeNull();
    expect(await harness.getCloseButton()).toBeNull();
  });

  it('should display Closed after closing a consumer-paused subscription', async () => {
    const initialSubscription = acceptedSubscription();
    const pausedSubscription = acceptedSubscription({ consumerStatus: SubscriptionConsumerStatusEnum.STOPPED });
    const closedSubscription = acceptedSubscription({
      status: 'CLOSED',
      consumerStatus: SubscriptionConsumerStatusEnum.STOPPED,
    });
    const harness = await init(initialSubscription);
    subscriptionService.get.mockReturnValueOnce(of(pausedSubscription)).mockReturnValueOnce(of(closedSubscription));

    await (await harness.getPauseButton())!.click();
    await fixture.whenStable();
    fixture.detectChanges();
    await (await harness.getCloseButton())!.click();
    const dialog = await rootLoader.getHarness(ConfirmDialogHarness);
    await dialog.confirm();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.getSummaryText()).toContain('Closed');
    expect(await harness.getSummaryText()).toContain('Unavailable');
    expect(await harness.getCredentialsText()).toContain('subscription is closed');
    expect(await harness.getCredentialsText()).not.toContain('subscription is paused');
  });

  it('should keep the subscription open when close is cancelled', async () => {
    const harness = await init();

    await (await harness.getCloseButton())!.click();
    const dialog = await rootLoader.getHarness(ConfirmDialogHarness);
    await dialog.cancel();

    expect(subscriptionService.close).not.toHaveBeenCalled();
  });

  it('should display an inline error when an action fails', async () => {
    const harness = await init();
    subscriptionService.changeConsumerStatus.mockReturnValue(throwError(() => new Error('Request failed')));

    await (await harness.getPauseButton())!.click();
    fixture.detectChanges();

    expect(await harness.getFeedbackText()).toContain('could not be paused');
    expect(await harness.getFeedbackAttribute('role')).toEqual('alert');
    expect(await harness.getFeedbackAttribute('aria-live')).toBeNull();
    expect(await (await harness.getPauseButton())!.isDisabled()).toBe(false);
  });

  it('should distinguish a successful action followed by a refresh failure', async () => {
    const harness = await init();
    subscriptionService.get.mockReturnValue(throwError(() => new Error('Refresh failed')));

    await (await harness.getPauseButton())!.click();
    fixture.detectChanges();

    expect(subscriptionService.changeConsumerStatus).toHaveBeenCalled();
    expect(await harness.getFeedbackText()).toContain('updated, but its latest details could not be loaded');
  });
});
