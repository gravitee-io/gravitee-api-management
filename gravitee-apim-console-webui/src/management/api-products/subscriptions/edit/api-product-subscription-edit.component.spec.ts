/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { set } from 'lodash';

import { ApiProductSubscriptionEditComponent } from './api-product-subscription-edit.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { fakeBasePlan, fakeSubscription, Subscription } from '../../../../entities/management-api-v2';
import { fakeApiKey } from '../../../../entities/management-api-v2/api-key';

const API_PRODUCT_ID = 'product-1';
const SUBSCRIPTION_ID = 'sub-1';
const PLAN_ID = 'plan-1';
const APP_ID = 'app-1';

function buildSubscription(overrides: Partial<Subscription> = {}): Subscription {
  return fakeSubscription({
    id: SUBSCRIPTION_ID,
    plan: fakeBasePlan({ id: PLAN_ID, security: { type: 'API_KEY' } }),
    status: 'ACCEPTED',
    application: {
      id: APP_ID,
      name: 'My App',
      description: 'Test app',
      domain: 'https://example.com',
      type: 'SIMPLE',
      primaryOwner: { id: 'owner-1', displayName: 'Owner One' },
      apiKeyMode: 'UNSPECIFIED',
    },
    ...overrides,
  });
}

describe('ApiProductSubscriptionEditComponent', () => {
  let fixture: ComponentFixture<ApiProductSubscriptionEditComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const snackBarService = { error: jest.fn(), success: jest.fn() };

  async function init(permissions: string[] = ['api_product-subscription-r', 'api_product-subscription-u']) {
    await TestBed.configureTestingModule({
      imports: [ApiProductSubscriptionEditComponent, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        { provide: GioTestingPermissionProvider, useValue: permissions },
        { provide: SnackBarService, useValue: snackBarService },
        {
          provide: Constants,
          useFactory: () => {
            const constants = { ...CONSTANTS_TESTING };
            set(constants, 'env.settings.plan.security.customApiKey.enabled', true);
            return constants;
          },
        },
        {
          provide: InteractivityChecker,
          useValue: { isFocusable: () => true, isTabbable: () => true },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ apiProductId: API_PRODUCT_ID, subscriptionId: SUBSCRIPTION_ID })),
            snapshot: { params: { apiProductId: API_PRODUCT_ID, subscriptionId: SUBSCRIPTION_ID } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductSubscriptionEditComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  }

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  function expectSubscriptionRequest(subscription = buildSubscription()) {
    const req = httpTestingController.expectOne(r => {
      if (!r.url.includes(`/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}`)) return false;
      const expands = r.params.get('expands');
      return expands === 'plan,application,subscribedBy';
    });
    req.flush(subscription);
    return subscription;
  }

  function expectApiKeysRequest() {
    const req = httpTestingController.expectOne(r =>
      r.url.includes(`/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/api-keys`),
    );
    req.flush({ data: [fakeApiKey({ key: 'test-key-1', revoked: false, expired: false })], pagination: { totalCount: 1 } });
  }

  describe('subscription details display', () => {
    beforeEach(async () => {
      await init();
    });

    it('should display subscription details', async () => {
      fixture.detectChanges();
      expectSubscriptionRequest();
      expectApiKeysRequest();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('[data-testid="subscription-id"]')?.textContent).toContain(SUBSCRIPTION_ID);
      expect(compiled.querySelector('[data-testid="subscription-status"]')?.textContent).toContain('ACCEPTED');
    });

    it('should display action buttons for accepted subscription', async () => {
      fixture.detectChanges();
      expectSubscriptionRequest();
      expectApiKeysRequest();
      fixture.detectChanges();

      const closeButton = await loader.getHarness(MatButtonHarness.with({ text: /Close subscription/ }));
      expect(closeButton).toBeTruthy();
    });
  });

  describe('close subscription', () => {
    beforeEach(async () => {
      await init();
    });

    it('should close subscription on confirm', async () => {
      fixture.detectChanges();
      expectSubscriptionRequest();
      expectApiKeysRequest();
      fixture.detectChanges();

      const closeButton = await loader.getHarness(MatButtonHarness.with({ text: /Close subscription/ }));
      await closeButton.click();

      const closeDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#confirmCloseSubscriptionDialog' }));
      const confirmBtn = await closeDialog.getHarness(MatButtonHarness.with({ text: /^Close$/ }));
      await confirmBtn.click();

      const closeReq = httpTestingController.expectOne(
        r => r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions/${SUBSCRIPTION_ID}/_close`,
      );
      expect(closeReq.request.method).toBe('POST');
      closeReq.flush(buildSubscription({ status: 'CLOSED' }));

      fixture.detectChanges();
      await fixture.whenStable();

      // After close, refreshSubscription triggers a reload: GET subscription then GET api-keys (plan is API_KEY)
      expectSubscriptionRequest(buildSubscription({ status: 'CLOSED' }));
      expectApiKeysRequest();

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription closed');
    });
  });

  describe('pending subscription', () => {
    beforeEach(async () => {
      await init();
    });

    it('should show validate and reject buttons for pending subscription', async () => {
      fixture.detectChanges();
      expectSubscriptionRequest(buildSubscription({ status: 'PENDING' }));
      expectApiKeysRequest();
      fixture.detectChanges();

      const validateButton = await loader.getHarness(MatButtonHarness.with({ text: /Validate subscription/ }));
      const rejectButton = await loader.getHarness(MatButtonHarness.with({ text: /Reject subscription/ }));
      expect(validateButton).toBeTruthy();
      expect(rejectButton).toBeTruthy();
    });
  });

  describe('read-only mode', () => {
    beforeEach(async () => {
      await init(['api_product-subscription-r']);
    });

    it('should not show action buttons without update permission', async () => {
      fixture.detectChanges();
      expectSubscriptionRequest();
      expectApiKeysRequest();
      fixture.detectChanges();

      const closeButtons = await loader.getAllHarnesses(MatButtonHarness.with({ text: /Close subscription/ }));
      expect(closeButtons.length).toBe(0);
    });
  });
});
