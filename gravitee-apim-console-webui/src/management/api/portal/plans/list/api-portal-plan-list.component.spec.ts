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
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatCellHarness, MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatButtonToggleHarness } from '@angular/material/button-toggle/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { GioConfirmAndValidateDialogHarness } from '@gravitee/ui-particles-angular';
import { castArray } from 'lodash';

import { ApiPortalPlanListComponent } from './api-portal-plan-list.component';

import { ApiPortalPlansModule } from '../api-portal-plans.module';
import { Api, ApiPlan } from '../../../../../entities/api';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { fakePlan } from '../../../../../entities/plan/plan.fixture';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { User as DeprecatedUser } from '../../../../../entities/user';
import { Subscription } from '../../../../../entities/subscription/subscription';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { PlanSecurityType, PLAN_STATUS } from '../../../../../entities/plan';

describe('ApiPortalPlanListComponent', () => {
  const API_ID = 'api#1';
  const anAPi = fakeApi({ id: API_ID });
  const fakeUiRouter = { go: jest.fn() };
  const currentUser = new DeprecatedUser();
  currentUser.userPermissions = ['api-plan-u', 'api-plan-r', 'api-plan-d'];

  let fixture: ComponentFixture<ApiPortalPlanListComponent>;
  let component: ApiPortalPlanListComponent;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiPortalPlansModule, NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
        {
          provide: InteractivityChecker,
          useValue: {
            isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('plansTable tests', () => {
    it('should display an empty table', fakeAsync(async () => {
      await initComponent([]);

      const { headerCells, rowCells } = await computePlansTableCells();
      expect(headerCells).toEqual([
        {
          'drag-icon': '',
          name: 'Name',
          security: 'Security',
          status: 'Status',
          'deploy-on': 'Deploy on',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([['There is no plan (yet).']]);
    }));

    it('should display a table with one row', fakeAsync(async () => {
      const plan = fakePlan({ tags: ['🙅', '🔑'] });
      await initComponent([plan]);

      const { headerCells, rowCells } = await computePlansTableCells();
      expect(headerCells).toEqual([
        {
          'drag-icon': '',
          name: 'Name',
          security: 'Security',
          status: 'Status',
          'deploy-on': 'Deploy on',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([['', 'Free Spaceshuttle', 'KEY_LESS', 'PUBLISHED', '🙅, 🔑', '']]);
    }));

    it('should search closed plan on click', fakeAsync(async () => {
      const goldPlan = fakePlan({ name: 'gold plan ⭐️' });
      await initComponent([goldPlan]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: /CLOSED/ })).then((btn) => btn.toggle());

      const closedPlan = fakePlan({ name: 'closed plan 🚪', status: 'CLOSED' });
      expectApiPlansListRequest([closedPlan], 'CLOSED');

      const { rowCells } = await computePlansTableCells();
      expect(rowCells).toEqual([['', 'closed plan 🚪', 'KEY_LESS', 'CLOSED', '', '']]);
    }));

    it('should search and not find any plan', fakeAsync(async () => {
      await initComponent([fakePlan()]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then((btn) => btn.toggle());

      expectApiPlansListRequest([], 'STAGING');

      const { rowCells } = await computePlansTableCells();
      expect(rowCells).toEqual([['There is no plan (yet).']]);
    }));
  });

  describe('drop tests', () => {
    it('should drop a plan and update his order', async () => {
      const plan1 = fakePlan({ name: 'Plan 1️⃣', order: 1 });
      const plan2 = fakePlan({ name: 'Plan 2️⃣', order: 2 });
      await initComponent([plan1, plan2]);

      component.dropRow({ previousIndex: 1, currentIndex: 0 } as any);

      expectApiPlanUpdateRequest({ ...plan2, order: 1 });
      expectApiPlansListRequest([
        { ...plan2, order: 1 },
        { ...plan1, order: 2 },
      ]);
    });

    it('should fail to update and reload plans', async () => {
      const plan1 = fakePlan({ name: 'Plan 1️⃣', order: 1 });
      const plan2 = fakePlan({ name: 'Plan 2️⃣', order: 2 });
      await initComponent([plan1, plan2]);
      const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');

      component.dropRow({ previousIndex: 1, currentIndex: 0 } as any);

      expectApiPlanUpdateRequestFail({ ...plan2, order: 1 });
      expectApiPlansListRequest([plan1, plan2]);
      expect(snackBarSpy).toHaveBeenCalled();
    });
  });

  describe('actions tests', () => {
    it('should navigate to edit when click on the action button', async () => {
      const plan = fakePlan();
      await initComponent([plan]);
      fakeUiRouter.go.mockReset();

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Edit the plan"]' })).then((btn) => btn.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.detail.portal.plan.edit', { planId: plan.id });
    });

    it('should navigate to new plan', async () => {
      await initComponent([]);
      fixture.componentInstance.isLoadingData = false;
      fakeUiRouter.go.mockReset();

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then((btn) => btn.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.detail.portal.plan.new');
    });

    it('should navigate to edit when click on the name', async () => {
      const plan = fakePlan();
      await initComponent([plan]);
      fakeUiRouter.go.mockReset();

      await loader
        .getHarness(MatCellHarness.with({ text: plan.name }))
        .then((btn) => btn.host())
        .then((host) => host.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.detail.portal.plan.edit', { planId: plan.id });
    });

    it('should navigate to design when click on the design button', async () => {
      const plan = fakePlan();
      await initComponent([plan]);
      fakeUiRouter.go.mockReset();

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Design the plan"]' })).then((btn) => btn.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.detail.design.flowsNg', { apiId: API_ID, flows: `${plan.id}_0` });
    });

    it('should publish the staging plan', async () => {
      const plan = fakePlan({ name: 'publish me ☁️️', status: 'STAGING' });
      await initComponent([plan]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then((btn) => btn.toggle());
      expectApiPlansListRequest([plan], 'STAGING');

      let table = await computePlansTableCells();
      expect(table.rowCells).toEqual([['', 'publish me ☁️️', 'KEY_LESS', 'STAGING', '', '']]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' })).then((btn) => btn.click());

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
      const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
      await confirmDialogSwitchButton.click();

      const updatedPlan: ApiPlan = { ...plan, status: 'PUBLISHED' };
      expectPlanGetRequest(updatedPlan);
      expectApiPlanPublishRequest(updatedPlan);
      expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);

      table = await computePlansTableCells();
      expect(table.rowCells).toEqual([['There is no plan (yet).']]);
    });

    it('should deprecate the published plan', async () => {
      const plan = fakePlan({ name: 'deprecate me 😥️', status: 'PUBLISHED' });
      await initComponent([plan]);

      let table = await computePlansTableCells();
      expect(table.rowCells).toEqual([['', 'deprecate me 😥️', 'KEY_LESS', 'PUBLISHED', '', '']]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Deprecate the plan"]' })).then((btn) => btn.click());

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#deprecatePlanDialog' }));
      const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Deprecate' }));
      await confirmDialogSwitchButton.click();

      const updatedPlan: ApiPlan = { ...plan, status: 'DEPRECATED' };
      expectPlanGetRequest(updatedPlan);
      expectApiPlanDeprecateRequest(updatedPlan);
      expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);

      table = await computePlansTableCells();
      expect(table.rowCells).toEqual([['There is no plan (yet).']]);
    });

    describe('close plan', () => {
      it('should close the published plan', async () => {
        const plan = fakePlan({ name: 'close me 🚪️', status: 'PUBLISHED' });
        await initComponent([plan]);

        let table = await computePlansTableCells();
        expect(table.rowCells).toEqual([['', 'close me 🚪️', 'KEY_LESS', 'PUBLISHED', '', '']]);

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' })).then((btn) => btn.click());

        expectGetApiPlanSubscriptionsRequest(plan.id);

        const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
        await confirmDialog.confirm();

        const updatedPlan: ApiPlan = { ...plan, status: 'CLOSED' };
        expectApiPlanCloseRequest(updatedPlan);
        expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);

        table = await computePlansTableCells();
        expect(table.rowCells).toEqual([['There is no plan (yet).']]);
      });

      it('should change delete plan button message', async () => {
        const plan = fakePlan({ name: 'key plan 🔑️', status: 'PUBLISHED', security: PlanSecurityType.API_KEY });
        await initComponent([plan]);

        const table = await computePlansTableCells();
        expect(table.rowCells).toEqual([['', 'key plan 🔑️', 'API_KEY', 'PUBLISHED', '', '']]);

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' })).then((btn) => btn.click());

        expectGetApiPlanSubscriptionsRequest(plan.id);

        const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
        expect(await rootLoader.getHarness(MatButtonHarness.with({ text: 'Yes, delete this plan' }))).toBeTruthy();
        await confirmDialog.confirm();

        const updatedPlan: ApiPlan = { ...plan, status: 'CLOSED' };
        expectApiPlanCloseRequest(updatedPlan);
        expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
      });
    });
  });

  describe('kubernetes api tests', () => {
    it('should access plan in read only', async () => {
      const kubernetesApi = fakeApi({ id: API_ID, definition_context: { origin: 'kubernetes' } });
      const plan = fakePlan({ api: API_ID, tags: ['tag1', 'tag2'] });
      await initComponent([plan], kubernetesApi);

      expect(component.isReadOnly).toBe(true);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="View the plan details"]' })).then((btn) => btn.click());

      expect(await loader.getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' }))).toHaveLength(0);
      expect(fakeUiRouter.go).toBeCalledWith('management.apis.detail.portal.plan.edit', { planId: plan.id });

      const { headerCells, rowCells } = await computePlansTableCells();
      expect(headerCells).toEqual([
        {
          name: 'Name',
          security: 'Security',
          status: 'Status',
          'deploy-on': 'Deploy on',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([['Free Spaceshuttle', 'KEY_LESS', 'PUBLISHED', 'tag1, tag2', '']]);
    });
  });

  async function initComponent(plans: ApiPlan[], api: Api = anAPi) {
    await TestBed.overrideProvider(UIRouterStateParams, { useValue: { apiId: api.id } }).compileComponents();
    fixture = TestBed.createComponent(ApiPortalPlanListComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    expectApiGetRequest(api);
    expectApiPlansListRequest(plans, [...PLAN_STATUS]);

    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  }

  async function computePlansTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  function expectApiPlansListRequest(plans: ApiPlan[] = [], status?: string | string[], security?: string) {
    httpTestingController
      .expectOne(
        `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans?${status ? `status=${castArray(status).join(',')}` : 'status=PUBLISHED'}${
          security ? `security=${security}` : ''
        }`,
        'GET',
      )
      .flush(plans);
    fixture.detectChanges();
  }

  function expectApiPlanUpdateRequest(plan: ApiPlan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans/${plan.id}`, 'PUT');
    expect(req.request.body).toEqual(plan);
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiPlanUpdateRequestFail(plan: ApiPlan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans/${plan.id}`, 'PUT');
    expect(req.request.body).toEqual(plan);
    req.error(new ErrorEvent('error'), { status: 400 });
    fixture.detectChanges();
  }

  function expectApiPlanPublishRequest(plan: ApiPlan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans/${plan.id}/_publish`, 'POST');
    expect(req.request.body).toEqual(plan);
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiPlanDeprecateRequest(plan: ApiPlan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans/${plan.id}/_deprecate`, 'POST');
    expect(req.request.body).toEqual(plan);
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiPlanCloseRequest(plan: ApiPlan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans/${plan.id}/_close`, 'POST');
    expect(req.request.body).toEqual({});
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectPlanGetRequest(plan: ApiPlan) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans/${plan.id}`, method: 'GET' }).flush(plan);
    fixture.detectChanges();
  }

  function expectGetApiPlanSubscriptionsRequest(planId: string, subscriptions: Subscription[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/subscriptions?plan=${planId}&status=accepted,pending,rejected,closed,paused`,
        method: 'GET',
      })
      .flush({
        data: subscriptions,
        metadata: {},
        page: {
          size: subscriptions.length,
        },
      });
    fixture.detectChanges();
  }
});
