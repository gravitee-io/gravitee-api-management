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
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SubscribeToApiProductComponent } from './subscribe-to-api-product.component';
import { SubscribeToApiProductHarness } from './subscribe-to-api-product.component.harness';
import { RadioCardHarness } from '../../../components/radio-card/radio-card.harness';
import { fakeApiProduct } from '../../../entities/api-product/api-product.fixture';
import { fakeApplication, fakeApplicationsResponse } from '../../../entities/application/application.fixture';
import { fakePlan } from '../../../entities/plan/plan.fixture';
import { fakeSubscription, fakeSubscriptionResponse, Subscription } from '../../../entities/subscription';
import { ApiProductsService } from '../../../services/api-products.service';
import { ApplicationService } from '../../../services/application.service';
import { SubscriptionService } from '../../../services/subscription.service';

describe('SubscribeToApiProductComponent', () => {
  let fixture: ComponentFixture<SubscribeToApiProductComponent>;
  let harness: SubscribeToApiProductHarness;
  let harnessLoader: HarnessLoader;
  let apiProductsService: { listPlans: jest.Mock };
  let applicationService: { list: jest.Mock };
  let subscriptionService: { list: jest.Mock; subscribe: jest.Mock };
  let matDialog: { open: jest.Mock };

  const apiProduct = fakeApiProduct({ id: 'api-product-id', name: 'Developer Product' });
  const apiKeyPlan = fakePlan({ id: 'api-key-plan', name: 'API Key', security: 'API_KEY', mode: 'STANDARD' });
  const application = fakeApplication({ id: 'application-id', name: 'Developer App', api_key_mode: 'EXCLUSIVE' });

  const init = async ({
    plans = [apiKeyPlan],
    plansError = undefined as Error | undefined,
    subscriptions = fakeSubscriptionResponse({ data: [], metadata: {} }),
    applications = fakeApplicationsResponse({ data: [application] }),
    applicationsError = undefined as Error | undefined,
  } = {}) => {
    apiProductsService = {
      listPlans: jest.fn().mockReturnValue(plansError ? throwError(() => plansError) : of({ data: plans })),
    };
    applicationService = {
      list: jest.fn().mockReturnValue(applicationsError ? throwError(() => applicationsError) : of(applications)),
    };
    subscriptionService = {
      list: jest.fn().mockReturnValue(of(subscriptions)),
      subscribe: jest.fn(),
    };
    matDialog = { open: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [SubscribeToApiProductComponent, MatIconTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ApiProductsService, useValue: apiProductsService },
        { provide: ApplicationService, useValue: applicationService },
        { provide: SubscriptionService, useValue: subscriptionService },
        { provide: MatDialog, useValue: matDialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscribeToApiProductComponent);
    fixture.componentRef.setInput('apiProduct', apiProduct);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscribeToApiProductHarness);
  };

  it('should load and display only plans returned for the API Product', async () => {
    await init({ plans: [apiKeyPlan, fakePlan({ id: 'jwt-plan', name: 'JWT', security: 'JWT' })] });

    expect(apiProductsService.listPlans).toHaveBeenCalledWith(apiProduct.id);
    expect(await harness.getText()).toContain('API Key');
    expect(await harness.getText()).toContain('JWT');
  });

  it('should display an empty state when no plan is available', async () => {
    await init({ plans: [] });

    expect(await harness.getText()).toContain('No subscription plan is currently available');
  });

  it('should preserve keyless behavior without creating a subscription', async () => {
    const keylessPlan = fakePlan({ id: 'keyless-plan', security: 'KEY_LESS' });
    await init({ plans: [keylessPlan] });

    await harness.selectPlan(keylessPlan.id);
    await harness.goToNextStep();

    expect(await harness.getText()).toContain('does not require a subscription');
    expect(await harness.hasSubscribeAction()).toBe(false);
    expect(applicationService.list).not.toHaveBeenCalled();

    fixture.componentInstance.subscribe();

    expect(subscriptionService.subscribe).not.toHaveBeenCalled();
  });

  it('should keep standalone API subscriptions independent from API Product eligibility', async () => {
    const duplicateProductApplication = fakeApplication({ id: 'duplicate-app', name: 'Duplicate Product App', api_key_mode: 'EXCLUSIVE' });
    const subscriptions = fakeSubscriptionResponse({
      data: [
        fakeSubscription({
          application: application.id,
          plan: apiKeyPlan.id,
          status: 'ACCEPTED',
          reference_id: 'included-api-id',
          reference_type: 'API',
        }),
        fakeSubscription({
          api: undefined,
          application: duplicateProductApplication.id,
          plan: apiKeyPlan.id,
          status: 'PENDING',
          reference_id: apiProduct.id,
          reference_type: 'API_PRODUCT',
        }),
      ],
      metadata: { [apiKeyPlan.id]: { securityType: 'API_KEY', planMode: 'STANDARD' } },
    });
    await init({
      subscriptions,
      applications: fakeApplicationsResponse({ data: [application, duplicateProductApplication] }),
    });

    await harness.selectPlan(apiKeyPlan.id);
    await harness.goToNextStep();
    await fixture.whenStable();
    fixture.detectChanges();

    const standaloneApplication = await harnessLoader.getHarness(RadioCardHarness.with({ title: application.name }));
    const duplicateApplication = await harnessLoader.getHarness(RadioCardHarness.with({ title: duplicateProductApplication.name }));
    expect(await standaloneApplication.isDisabled()).toBe(false);
    expect(await duplicateApplication.isDisabled()).toBe(true);
  });

  it('should disable an application missing credentials required by the selected plan', async () => {
    const oauthPlan = fakePlan({ id: 'oauth-plan', security: 'OAUTH2' });
    const applicationWithoutClientId = fakeApplication({ id: 'no-client-id', name: 'No Client ID', hasClientId: false });
    await init({ plans: [oauthPlan], applications: fakeApplicationsResponse({ data: [applicationWithoutClientId] }) });

    await harness.selectPlan(oauthPlan.id);
    await harness.goToNextStep();
    await fixture.whenStable();
    fixture.detectChanges();

    const applicationCard = await harnessLoader.getHarness(RadioCardHarness.with({ title: applicationWithoutClientId.name }));
    expect(await applicationCard.isDisabled()).toBe(true);
  });

  it.each([
    ['PENDING', 'awaiting approval'],
    ['ACCEPTED', 'now subscribed'],
  ] as const)('should submit the existing payload and show an inline %s confirmation', async (status, expectedText) => {
    await init();
    subscriptionService.subscribe.mockReturnValue(
      of(
        fakeSubscription({
          api: undefined,
          application: application.id,
          plan: apiKeyPlan.id,
          reference_id: apiProduct.id,
          reference_type: 'API_PRODUCT',
          status,
        }),
      ),
    );

    await selectPlanAndApplication();
    await harness.subscribe();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(subscriptionService.subscribe).toHaveBeenCalledWith({ application: application.id, plan: apiKeyPlan.id });
    expect(subscriptionService.subscribe.mock.calls[0][0]).not.toHaveProperty('apiProductId');
    expect(await harness.hasConfirmation()).toBe(true);
    expect(await harness.getText()).toContain(expectedText);
  });

  it('should capture a plan request comment', async () => {
    const commentPlan = fakePlan({ ...apiKeyPlan, comment_required: true, comment_question: 'Why do you need access?' });
    await init({ plans: [commentPlan] });
    matDialog.open.mockReturnValue({ afterClosed: () => of('Business integration') });
    subscriptionService.subscribe.mockReturnValue(of(createdSubscription('PENDING', commentPlan.id)));

    await selectPlanAndApplication(commentPlan.id);
    await harness.subscribe();

    expect(matDialog.open).toHaveBeenCalled();
    expect(subscriptionService.subscribe).toHaveBeenCalledWith({
      application: application.id,
      plan: commentPlan.id,
      request: 'Business integration',
    });
  });

  it('should submit PUSH consumer configuration', async () => {
    const pushPlan = fakePlan({ id: 'push-plan', mode: 'PUSH', security: 'API_KEY' });
    await init({ plans: [pushPlan] });
    subscriptionService.subscribe.mockReturnValue(of(createdSubscription('ACCEPTED', pushPlan.id)));

    await harness.selectPlan(pushPlan.id);
    await harness.goToNextStep();
    await fixture.whenStable();
    fixture.detectChanges();
    await (await harnessLoader.getHarness(RadioCardHarness.with({ title: application.name }))).select();
    await harness.goToNextStep();
    fixture.componentInstance.consumerConfigurationFormChanges({
      isValid: true,
      value: {
        channel: 'orders',
        consumerConfiguration: {
          callbackUrl: 'https://example.com/callback',
          headers: [],
          retry: { retryOption: 'No Retry' },
          ssl: { hostnameVerifier: false, trustAll: false },
          auth: { type: 'none' },
        },
      },
    });
    await harness.goToNextStep();
    await harness.subscribe();

    expect(subscriptionService.subscribe).toHaveBeenCalledWith({
      application: application.id,
      plan: pushPlan.id,
      configuration: {
        entrypointId: 'webhook',
        channel: 'orders',
        entrypointConfiguration: {
          callbackUrl: 'https://example.com/callback',
          headers: [],
          retry: { retryOption: 'No Retry' },
          ssl: { hostnameVerifier: false, trustAll: false },
          auth: { type: 'none' },
        },
      },
    });
  });

  it('should expose plan loading errors and retry', async () => {
    await init({ plansError: new Error('plans unavailable') });

    expect(await harness.hasError()).toBe(true);

    apiProductsService.listPlans.mockReturnValue(of({ data: [apiKeyPlan] }));
    fixture.componentInstance.retryPlans();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(apiProductsService.listPlans).toHaveBeenCalledTimes(2);
    expect(await harness.getText()).toContain(apiKeyPlan.name);
  });

  it('should fail closed when eligibility cannot be loaded and allow retry', async () => {
    await init({ applicationsError: new Error('applications unavailable') });

    await harness.selectPlan(apiKeyPlan.id);
    await harness.goToNextStep();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.hasError()).toBe(true);

    applicationService.list.mockReturnValue(of(fakeApplicationsResponse({ data: [application] })));
    fixture.componentInstance.retryApplications();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harnessLoader.getHarness(RadioCardHarness.with({ title: application.name }))).toBeDefined();
  });

  it('should display a submission error and allow retry', async () => {
    const consoleError = jest.spyOn(console, 'error').mockImplementation();
    await init();
    subscriptionService.subscribe
      .mockReturnValueOnce(throwError(() => new Error('subscription unavailable')))
      .mockReturnValueOnce(of(createdSubscription('ACCEPTED', apiKeyPlan.id)));
    await selectPlanAndApplication();

    await harness.subscribe();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(await harness.hasError()).toBe(true);

    await harness.subscribe();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(await harness.hasConfirmation()).toBe(true);

    consoleError.mockRestore();
  });

  async function selectPlanAndApplication(planId = apiKeyPlan.id): Promise<void> {
    await harness.selectPlan(planId);
    await harness.goToNextStep();
    await fixture.whenStable();
    fixture.detectChanges();
    await (await harnessLoader.getHarness(RadioCardHarness.with({ title: application.name }))).select();
    await harness.goToNextStep();
    fixture.detectChanges();
  }

  function createdSubscription(status: Subscription['status'], planId: string): Subscription {
    return fakeSubscription({
      api: undefined,
      application: application.id,
      plan: planId,
      reference_id: apiProduct.id,
      reference_type: 'API_PRODUCT',
      status,
    });
  }
});
