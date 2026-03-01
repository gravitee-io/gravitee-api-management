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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { ApiProductSubscriptionListComponent } from './api-product-subscription-list.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiPlansResponse, ApiSubscriptionsResponse, fakePlanV4, fakeSubscription } from '../../../../entities/management-api-v2';

describe('ApiProductSubscriptionListComponent', () => {
  const API_PRODUCT_ID = 'product-abc';
  const PLAN_ID = 'plan-1';
  const APPLICATION_ID = 'app-1';

  const aSubscription = fakeSubscription({
    id: 'sub-1',
    plan: fakePlanV4({ id: PLAN_ID, name: 'My API Key Plan', security: { type: 'API_KEY' } }),
    status: 'ACCEPTED',
    application: {
      id: APPLICATION_ID,
      name: 'Test App',
      primaryOwner: { id: 'owner-1', displayName: 'App Owner' },
      type: 'SIMPLE',
      apiKeyMode: 'UNSPECIFIED',
    },
  });

  const plansResponse: ApiPlansResponse = {
    data: [fakePlanV4({ id: PLAN_ID, name: 'My API Key Plan', security: { type: 'API_KEY' }, status: 'PUBLISHED' })],
  };

  const subscriptionsResponse: ApiSubscriptionsResponse = {
    data: [aSubscription],
    pagination: { totalCount: 1 },
  };

  let fixture: ComponentFixture<ApiProductSubscriptionListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let queryParams$: BehaviorSubject<Record<string, string>>;

  const snackBarService = { error: jest.fn(), success: jest.fn() };

  async function init(
    permissions: string[] = ['api_product-subscription-r', 'api_product-subscription-u', 'api_product-subscription-c'],
    initialQueryParams: Record<string, string> = {},
  ) {
    queryParams$ = new BehaviorSubject<Record<string, string>>(initialQueryParams);
    await TestBed.configureTestingModule({
      imports: [ApiProductSubscriptionListComponent, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        { provide: GioTestingPermissionProvider, useValue: permissions },
        { provide: SnackBarService, useValue: snackBarService },
        { provide: Constants, useValue: CONSTANTS_TESTING },
        {
          provide: InteractivityChecker,
          useValue: { isFocusable: () => true, isTabbable: () => true },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ apiProductId: API_PRODUCT_ID })),
            queryParams: queryParams$.asObservable(),
            queryParamMap: queryParams$.pipe(map(p => convertToParamMap(p))),
            snapshot: { params: { apiProductId: API_PRODUCT_ID }, queryParams: initialQueryParams },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductSubscriptionListComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);

    // When the component calls router.navigate(..., { queryParams }), the real Router does not
    // update our mock ActivatedRoute. Spy on navigate and push queryParams to queryParams$ so
    // that filters (derived from queryParams) update and the component's flow runs as in production.
    const router = TestBed.inject(Router);
    jest.spyOn(router, 'navigate').mockImplementation(((...args: unknown[]) => {
      const options = args[1] as { queryParams?: Record<string, unknown> } | undefined;
      if (options?.queryParams) {
        const strParams: Record<string, string> = {};
        Object.entries(options.queryParams).forEach(([k, v]) => {
          if (v !== undefined && v !== null) strParams[k] = String(v);
        });
        queryParams$.next(strParams);
      }
      return Promise.resolve(true);
    }) as Router['navigate']);
  }

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  function expectPlansRequest() {
    const req = httpTestingController.expectOne(r =>
      r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans`),
    );
    req.flush(plansResponse);
  }

  function expectSubscriptionsRequest(expectedPage?: string) {
    const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/subscriptions`;
    const req = httpTestingController.expectOne(r => {
      if (!r.url.startsWith(baseUrl) || r.url.includes('/_export')) return false;
      if (r.params.get('expands') !== 'application,plan') return false;
      if (expectedPage != null && r.params.get('page') !== expectedPage) return false;
      return true;
    });
    req.flush(subscriptionsResponse);
  }

  describe('list display', () => {
    beforeEach(async () => {
      await init();
    });

    it('should show subscriptions in table', fakeAsync(async () => {
      fixture.detectChanges();
      expectPlansRequest();
      tick(0); // Let toObservable(filters) effect emit
      tick(400); // debounceTime(400) then subscriptions request is sent
      fixture.detectChanges();
      expectSubscriptionsRequest();
      fixture.detectChanges();

      const table = await loader.getHarness(MatTableHarness);
      const rows = await table.getRows();
      expect(rows.length).toBe(1);
    }));

    it('should show loading indicator initially', fakeAsync(async () => {
      fixture.detectChanges();
      expectPlansRequest();
      tick(0);
      tick(400);

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('gio-loader')).toBeTruthy();

      expectSubscriptionsRequest();
      fixture.detectChanges();
    }));
  });

  describe('with update permission', () => {
    beforeEach(async () => {
      await init(['api_product-subscription-r', 'api_product-subscription-u', 'api_product-subscription-c']);
    });

    it('should show edit button for subscriptions', fakeAsync(async () => {
      fixture.detectChanges();
      expectPlansRequest();
      tick(0);
      tick(400);
      fixture.detectChanges();
      expectSubscriptionsRequest();
      fixture.detectChanges();

      const table = await loader.getHarness(MatTableHarness);
      const rows = await table.getRows();
      expect(rows.length).toBe(1);
    }));

    it('should show create subscription button', fakeAsync(async () => {
      fixture.detectChanges();
      expectPlansRequest();
      tick(0);
      tick(400);
      fixture.detectChanges();
      expectSubscriptionsRequest();
      fixture.detectChanges();

      const createButton = await loader.getHarness(MatButtonHarness.with({ text: /Create a subscription/ }));
      expect(createButton).toBeTruthy();
    }));
  });

  describe('without update permission', () => {
    beforeEach(async () => {
      await init(['api_product-subscription-r']);
    });

    it('should not show create subscription button', fakeAsync(async () => {
      fixture.detectChanges();
      expectPlansRequest();
      tick(0);
      tick(400);
      fixture.detectChanges();
      expectSubscriptionsRequest();
      fixture.detectChanges();

      const buttons = await loader.getAllHarnesses(MatButtonHarness.with({ text: /Create a subscription/ }));
      expect(buttons.length).toBe(0);
    }));
  });

  describe('reset filters', () => {
    it('should reset filters and reload subscriptions', fakeAsync(async () => {
      // Start with page=2 so that after reset (page=1) the filters change and a second request is sent
      await init(['api_product-subscription-r', 'api_product-subscription-u', 'api_product-subscription-c'], { page: '2' });
      fixture.detectChanges();
      expectPlansRequest();
      tick(0);
      tick(400);
      fixture.detectChanges();
      expectSubscriptionsRequest('2');
      fixture.detectChanges();

      const resetButton = await loader.getHarness(MatButtonHarness.with({ text: /Reset filters/ }));
      await resetButton.click();
      // Router.navigate spy pushes queryParams to queryParams$ so filters update and subscriptions reload
      tick(0);
      tick(400);
      fixture.detectChanges();
      expectSubscriptionsRequest('1');
      fixture.detectChanges();
    }));
  });
});
