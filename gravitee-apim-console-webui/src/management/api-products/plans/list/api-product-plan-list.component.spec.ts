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
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatButtonToggleHarness } from '@angular/material/button-toggle/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmAndValidateDialogHarness, GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { set } from 'lodash';

import { ApiProductPlanListComponent } from './api-product-plan-list.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiPlansResponse, Plan, PLAN_STATUS, fakePlanV4 } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

describe('ApiProductPlanListComponent', () => {
  const API_PRODUCT_ID = 'product-abc';

  let fixture: ComponentFixture<ApiProductPlanListComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;
  const snackBarService = { error: jest.fn(), success: jest.fn() };

  async function init(permissions: string[] = ['api_product-plan-u', 'api_product-plan-r', 'api_product-plan-c']) {
    await TestBed.configureTestingModule({
      imports: [ApiProductPlanListComponent, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        { provide: GioTestingPermissionProvider, useValue: permissions },
        { provide: SnackBarService, useValue: snackBarService },
        {
          provide: InteractivityChecker,
          useValue: { isFocusable: () => true },
        },
        {
          provide: Constants,
          useFactory: () => {
            const constants = { ...CONSTANTS_TESTING };
            set(constants, 'env.settings.plan.security', {
              apikey: { enabled: true },
              jwt: { enabled: true },
              mtls: { enabled: true },
              keyless: { enabled: true },
              oauth2: { enabled: true },
            });
            return constants;
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ apiProductId: API_PRODUCT_ID })),
            queryParamMap: of(convertToParamMap({ status: 'PUBLISHED' })),
            snapshot: { params: { apiProductId: API_PRODUCT_ID }, queryParams: { status: 'PUBLISHED' } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductPlanListComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  }

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  function flushPlansList(plans: Plan[], statuses = [...PLAN_STATUS]) {
    const response: ApiPlansResponse = { data: plans };
    httpTestingController
      .expectOne(
        `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans?page=1&perPage=9999&statuses=${statuses.join(',')}&fields=-flow`,
      )
      .flush(response);
    fixture.detectChanges();
  }

  describe('plan list table', () => {
    it('displays empty state when product has no plans', fakeAsync(async () => {
      await init();
      fixture.detectChanges();
      flushPlansList([]);

      const table = await loader.getHarness(MatTableHarness);
      expect(await (await table.host()).text()).toContain('There is no plan (yet).');
    }));

    it('displays plan rows when plans are loaded', fakeAsync(async () => {
      await init();
      fixture.detectChanges();
      const plan = { ...fakePlanV4({ name: 'JWT Plan', security: { type: 'JWT' }, status: 'PUBLISHED' }) };
      flushPlansList([plan]);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));
      const rows = await table.getRows();
      expect(rows).toHaveLength(1);
      const [row] = await parallel(() => rows.map(r => r.getCellTextByIndex()));
      expect(row).toContain('JWT Plan');
    }));

    it('omits deploy-on column for API product plans table', fakeAsync(async () => {
      await init();
      fixture.detectChanges();
      flushPlansList([]);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));
      const headerRows = await table.getHeaderRows();
      const [headers] = await parallel(() => headerRows.map(r => r.getCellTextByColumnName()));
      expect(headers['deploy-on']).toBeUndefined();
    }));
  });

  describe('plan type menu', () => {
    it('add plan menu shows only API Key JWT and mTLS types', fakeAsync(async () => {
      await init();
      fixture.detectChanges();
      flushPlansList([]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then(btn => btn.click());
      const menu = await loader.getHarness(MatMenuHarness);
      const items = await menu.getItems();
      const texts = await parallel(() => items.map(i => i.getText()));

      expect(texts).toEqual(expect.arrayContaining(['API Key', 'JWT', 'mTLS']));
      expect(texts).not.toContain('Keyless (public)');
      expect(texts).not.toContain('OAuth2');
      expect(texts).not.toContain('Push plan');
    }));
  });

  describe('read-only mode', () => {
    it('hides add plan button when user has only read permission', fakeAsync(async () => {
      await init(['api_product-plan-r']);
      fixture.detectChanges();
      flushPlansList([]);

      const btn = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' }));
      expect(btn).toBeNull();
    }));
  });

  describe('status filter', () => {
    it('reloads table when status filter is changed to STAGING', fakeAsync(async () => {
      await init();
      fixture.detectChanges();
      flushPlansList([fakePlanV4({ status: 'PUBLISHED' })]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then(btn => btn.toggle());

      const stagingPlan = fakePlanV4({ name: 'Staging Plan', status: 'STAGING', security: { type: 'API_KEY' } });
      flushPlansList([stagingPlan], ['STAGING']);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));
      const rows = await table.getRows();
      expect(rows).toHaveLength(1);
    }));
  });

  describe('plan lifecycle actions', () => {
    it('publishes staging plan after user confirms in dialog', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const plan = fakePlanV4({ name: 'New Plan', status: 'STAGING', security: { type: 'API_KEY' } });
      flushPlansList([plan]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then(btn => btn.toggle());
      flushPlansList([plan], ['STAGING']);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' })).then(btn => btn.click());

      const dialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
      await dialog.getHarness(MatButtonHarness.with({ text: 'Publish' })).then(btn => btn.click());

      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan.id}/_publish`, 'POST')
        .flush({ ...plan, status: 'PUBLISHED' });
      fixture.detectChanges();
      flushPlansList([{ ...plan, status: 'PUBLISHED' }]);

      expect(snackBarService.success).toHaveBeenCalledWith(expect.stringContaining('published with success'));
    }));

    it('deprecates published plan after user confirms in dialog', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const plan = fakePlanV4({ name: 'Active Plan', status: 'PUBLISHED', security: { type: 'JWT' } });
      flushPlansList([plan]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Deprecate the plan"]' })).then(btn => btn.click());

      const dialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#deprecatePlanDialog' }));
      await dialog.getHarness(MatButtonHarness.with({ text: 'Deprecate' })).then(btn => btn.click());

      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan.id}/_deprecate`, 'POST')
        .flush({ ...plan, status: 'DEPRECATED' });
      fixture.detectChanges();
      flushPlansList([{ ...plan, status: 'DEPRECATED' }]);

      expect(snackBarService.success).toHaveBeenCalledWith(expect.stringContaining('deprecated with success'));
    }));

    it('closes plan after user confirms in validation dialog', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const plan = fakePlanV4({ name: 'Close Me', status: 'PUBLISHED', security: { type: 'API_KEY' } });
      flushPlansList([plan]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' })).then(btn => btn.click());

      const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
      await confirmDialog.confirm();

      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan.id}/_close`, 'POST')
        .flush({ ...plan, status: 'CLOSED' });
      fixture.detectChanges();
      flushPlansList([{ ...plan, status: 'CLOSED' }]);

      expect(snackBarService.success).toHaveBeenCalledWith(expect.stringContaining('closed with success'));
    }));

    it('does not call publish API when user cancels publish dialog', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const plan = fakePlanV4({ name: 'New Plan', status: 'STAGING', security: { type: 'API_KEY' } });
      flushPlansList([plan]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then(btn => btn.toggle());
      flushPlansList([plan], ['STAGING']);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' })).then(btn => btn.click());

      const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await dialog.cancel();

      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan.id}/_publish`);
      expect(snackBarService.success).not.toHaveBeenCalled();
    }));

    it('does not call deprecate API when user cancels deprecate dialog', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const plan = fakePlanV4({ name: 'Active Plan', status: 'PUBLISHED', security: { type: 'JWT' } });
      flushPlansList([plan]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Deprecate the plan"]' })).then(btn => btn.click());

      const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await dialog.cancel();

      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan.id}/_deprecate`);
      expect(snackBarService.success).not.toHaveBeenCalled();
    }));

    it('does not call close API when user cancels close dialog', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const plan = fakePlanV4({ name: 'Close Me', status: 'PUBLISHED', security: { type: 'API_KEY' } });
      flushPlansList([plan]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' })).then(btn => btn.click());

      const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
      await confirmDialog.cancel();

      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan.id}/_close`);
      expect(snackBarService.success).not.toHaveBeenCalled();
    }));

    it('shows error snackbar and does not navigate when publish plan API fails', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const plan = fakePlanV4({ name: 'New Plan', status: 'STAGING', security: { type: 'API_KEY' } });
      flushPlansList([plan]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then(btn => btn.toggle());
      flushPlansList([plan], ['STAGING']);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' })).then(btn => btn.click());

      const dialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
      await dialog.getHarness(MatButtonHarness.with({ text: 'Publish' })).then(btn => btn.click());

      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan.id}/_publish`)
        .flush({ message: 'Publish failed' }, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(snackBarService.error).toHaveBeenCalledWith('Publish failed');
      expect(snackBarService.success).not.toHaveBeenCalled();
    }));

    it('shows error snackbar and does not navigate when deprecate plan API fails', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const plan = fakePlanV4({ name: 'Active Plan', status: 'PUBLISHED', security: { type: 'JWT' } });
      flushPlansList([plan]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Deprecate the plan"]' })).then(btn => btn.click());

      const dialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#deprecatePlanDialog' }));
      await dialog.getHarness(MatButtonHarness.with({ text: 'Deprecate' })).then(btn => btn.click());

      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan.id}/_deprecate`)
        .flush({ message: 'Deprecate failed' }, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(snackBarService.error).toHaveBeenCalledWith('Deprecate failed');
      expect(snackBarService.success).not.toHaveBeenCalled();
    }));

    it('shows error snackbar and does not navigate when close plan API fails', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const plan = fakePlanV4({ name: 'Close Me', status: 'PUBLISHED', security: { type: 'API_KEY' } });
      flushPlansList([plan]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' })).then(btn => btn.click());

      const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
      await confirmDialog.confirm();

      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan.id}/_close`)
        .flush({ message: 'Close failed' }, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(snackBarService.error).toHaveBeenCalledWith('Close failed');
      expect(snackBarService.success).not.toHaveBeenCalled();
    }));
  });

  describe('selecting a plan type navigates to create route', () => {
    it('navigates to new plan route with selected plan type as query param', fakeAsync(async () => {
      await init();
      fixture.detectChanges();
      flushPlansList([]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then(btn => btn.click());
      const menu = await loader.getHarness(MatMenuHarness);
      await menu.clickItem({ text: 'API Key' });

      expect(routerNavigateSpy).toHaveBeenCalledWith(
        ['./new'],
        expect.objectContaining({ queryParams: { selectedPlanMenuItem: 'API_KEY' } }),
      );
    }));
  });

  describe('plan reorder', () => {
    it('sends PUT with new order then reloads list and table reflects new order', fakeAsync(async () => {
      await init();
      fixture.detectChanges();
      const plan1 = fakePlanV4({ id: 'p1', name: 'First', order: 1, status: 'PUBLISHED', security: { type: 'API_KEY' } });
      const plan2 = fakePlanV4({ id: 'p2', name: 'Second', order: 2, status: 'PUBLISHED', security: { type: 'JWT' } });
      flushPlansList([plan1, plan2]);

      // No CDK drag-drop harness; simulate reorder to test business logic (PUT order, then reload)
      const dropEvent = { previousIndex: 0, currentIndex: 1 } as CdkDragDrop<string[]>;
      fixture.componentInstance['onPlanReordered'](dropEvent);
      tick();
      fixture.detectChanges();

      const putReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan1.id}`,
      });
      expect(putReq.request.body.order).toBe(2);
      putReq.flush({ ...plan1, order: 2 });
      tick();

      httpTestingController
        .expectOne(
          `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans?page=1&perPage=9999&statuses=${PLAN_STATUS.join(',')}&fields=-flow`,
        )
        .flush({ data: [plan2, { ...plan1, order: 2 }] });
      fixture.detectChanges();

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));
      const rows = await table.getRows();
      const [firstRow, secondRow] = await parallel(() => rows.map(r => r.getCellTextByIndex()));
      expect(firstRow).toContain('Second');
      expect(secondRow).toContain('First');
    }));

    it('shows error snackbar when reorder PUT fails and does not reload', fakeAsync(async () => {
      await init();
      fixture.detectChanges();
      const plan1 = fakePlanV4({ id: 'p1', name: 'First', order: 1, status: 'PUBLISHED', security: { type: 'API_KEY' } });
      const plan2 = fakePlanV4({ id: 'p2', name: 'Second', order: 2, status: 'PUBLISHED', security: { type: 'JWT' } });
      flushPlansList([plan1, plan2]);

      const dropEvent = { previousIndex: 0, currentIndex: 1 } as CdkDragDrop<string[]>;
      fixture.componentInstance['onPlanReordered'](dropEvent); // Simulate reorder (no drag harness)
      tick();
      fixture.detectChanges();

      const putReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${plan1.id}`,
      });
      putReq.flush({ message: 'Cannot reorder' }, { status: 400, statusText: 'Bad Request' });
      tick();

      expect(snackBarService.error).toHaveBeenCalledWith('Cannot reorder');
    }));
  });

  describe('load plans error handling', () => {
    it('shows error and empty state when list plans request fails', fakeAsync(async () => {
      await init();
      fixture.detectChanges();

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans?page=1&perPage=9999&statuses=${PLAN_STATUS.join(',')}&fields=-flow`,
      );
      req.flush({ message: 'An error occurred while loading plans.' }, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(snackBarService.error).toHaveBeenCalledWith('An error occurred while loading plans.');
      const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));
      expect(await (await table.host()).text()).toContain('There is no plan (yet).');
    }));
  });

  describe('security type labels', () => {
    it('displays mTLS and API Key labels in type column', fakeAsync(async () => {
      await init();
      fixture.detectChanges();
      const mtlsPlan = fakePlanV4({ name: 'mTLS Plan', security: { type: 'MTLS' }, status: 'PUBLISHED' });
      const apiKeyPlan = fakePlanV4({ name: 'API Key Plan', security: { type: 'API_KEY' }, status: 'PUBLISHED' });
      flushPlansList([mtlsPlan, apiKeyPlan]);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));
      const rows = await table.getRows();
      expect(rows).toHaveLength(2);
      const [row1, row2] = await parallel(() => rows.map(r => r.getCellTextByIndex()));
      expect(row1).toContain('mTLS Plan');
      expect(row1).toContain('mTLS');
      expect(row2).toContain('API Key Plan');
      expect(row2).toContain('API Key');
    }));
  });

  describe('missing apiProductId', () => {
    it('shows empty table and makes no HTTP request when apiProductId is absent', fakeAsync(async () => {
      await TestBed.configureTestingModule({
        imports: [ApiProductPlanListComponent, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
        providers: [
          { provide: GioTestingPermissionProvider, useValue: ['api_product-plan-u', 'api_product-plan-r'] },
          { provide: SnackBarService, useValue: snackBarService },
          { provide: InteractivityChecker, useValue: { isFocusable: () => true } },
          {
            provide: Constants,
            useFactory: () => {
              const constants = { ...CONSTANTS_TESTING };
              set(constants, 'env.settings.plan.security', {
                apikey: { enabled: true },
                jwt: { enabled: true },
                mtls: { enabled: true },
              });
              return constants;
            },
          },
          {
            provide: ActivatedRoute,
            useValue: {
              paramMap: of(convertToParamMap({})),
              queryParamMap: of(convertToParamMap({ status: 'PUBLISHED' })),
              snapshot: { params: {}, queryParams: { status: 'PUBLISHED' } },
            },
          },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ApiProductPlanListComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();

      // No HTTP call should be made when apiProductId is absent; verify() in afterEach confirms this
      const table = await loader.getHarness(MatTableHarness);
      expect(await (await table.host()).text()).toContain('There is no plan (yet).');
    }));
  });
});
