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
import { castArray, set } from 'lodash';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { UIRouterGlobals } from '@uirouter/core';

import { ApiGeneralPlanListComponent } from './api-general-plan-list.component';

import { ApiGeneralPlansModule } from '../api-general-plans.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { AjsRootScope, CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../../../entities/user';
import { Subscription } from '../../../../../entities/subscription/subscription';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import {
  Api,
  ApiPlansResponse,
  fakeApiV1,
  fakeApiV2,
  fakeApiV4,
  fakePlanV2,
  fakePlanV4,
  Plan,
  PLAN_STATUS,
} from '../../../../../entities/management-api-v2';

describe('ApiGeneralPlanListComponent', () => {
  const API_ID = 'api#1';
  const anAPi = fakeApiV2({ id: API_ID });
  const fakeUiRouter = { go: jest.fn() };
  const currentUser = new DeprecatedUser();
  currentUser.userPermissions = ['api-plan-u', 'api-plan-r', 'api-plan-d'];

  let fixture: ComponentFixture<ApiGeneralPlanListComponent>;
  let component: ApiGeneralPlanListComponent;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const fakeRootScope = { $broadcast: jest.fn(), $on: jest.fn() };
  const fakeAjsGlobals = { current: { data: { baseRouteState: 'management.apis' } } };

  const init = async (ajsGlobals: any = {}) => {
    await TestBed.configureTestingModule({
      imports: [ApiGeneralPlansModule, NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule],
      providers: [
        { provide: AjsRootScope, useValue: fakeRootScope },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
        {
          provide: InteractivityChecker,
          useValue: {
            isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          },
        },
        {
          provide: 'Constants',
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(constants, 'env.settings.plan.security', {
              apikey: {
                enabled: true,
              },
              jwt: {
                enabled: true,
              },
              keyless: {
                enabled: true,
              },
              oauth2: {
                enabled: true,
              },
              customApiKey: {
                enabled: true,
              },
              sharedApiKey: {
                enabled: true,
              },
              push: {
                enabled: true,
              },
            });
            return constants;
          },
        },
        {
          provide: UIRouterGlobals,
          useValue: ajsGlobals,
        },
      ],
    }).compileComponents();
  };

  beforeEach(async () => {
    await init();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('plansTable tests', () => {
    it('should display an empty table', fakeAsync(async () => {
      await initComponent([]);

      const { headerCells, rowCells } = await computePlansTableCells();
      expect(headerCells).toEqual([
        {
          'drag-icon': '',
          name: 'Name',
          status: 'Status',
          'deploy-on': 'Deploy on',
          type: 'Type',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([['There is no plan (yet).']]);
    }));

    it('should display a table with one row', fakeAsync(async () => {
      const plan = fakePlanV2({ tags: ['🙅', '🔑'] });
      await initComponent([plan]);

      const { headerCells, rowCells } = await computePlansTableCells();
      expect(headerCells).toEqual([
        {
          'drag-icon': '',
          name: 'Name',
          status: 'Status',
          'deploy-on': 'Deploy on',
          type: 'Type',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([['', 'Default plan', 'API_KEY', 'PUBLISHED', '🙅, 🔑', '']]);
    }));

    it('should not display PUSH plan option for V2 APIs', fakeAsync(async () => {
      await initComponent([]);
      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then((btn) => btn.click());

      const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
      const items = await planSecurityDropdown.getItems();
      const availablePlans = await Promise.all(items.map((item) => item.getText()));
      expect(availablePlans).not.toContain('Push plan');
    }));

    it('should display all plan options for V4 APIs with only HTTP and SUBSCRIPTION listeners', fakeAsync(async () => {
      const v4Api = fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP' }, { type: 'SUBSCRIPTION' }] });
      await initComponent([], v4Api);
      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then((btn) => btn.click());

      const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
      const items = await planSecurityDropdown.getItems();
      const availablePlans = await Promise.all(items.map((item) => item.getText()));
      expect(availablePlans).toEqual(['OAuth2', 'JWT', 'API Key', 'Keyless (public)', 'Push plan']);
    }));

    it('should not display PUSH plan option for V4 APIs with only HTTP listeners', fakeAsync(async () => {
      const v4Api = fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP' }, { type: 'TCP' }] });
      await initComponent([], v4Api);
      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then((btn) => btn.click());

      const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
      const items = await planSecurityDropdown.getItems();
      const availablePlans = await Promise.all(items.map((item) => item.getText()));
      expect(availablePlans).toEqual(['OAuth2', 'JWT', 'API Key', 'Keyless (public)']);
    }));

    it('should display only PUSH plan option for V4 APIs with only SUBSCRIPTION listeners', fakeAsync(async () => {
      const v4Api = fakeApiV4({ id: API_ID, listeners: [{ type: 'SUBSCRIPTION' }] });
      await initComponent([], v4Api);
      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then((btn) => btn.click());

      const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
      const items = await planSecurityDropdown.getItems();
      const availablePlans = await Promise.all(items.map((item) => item.getText()));
      expect(availablePlans).toEqual(['Push plan']);
    }));

    it('should search closed plan on click', fakeAsync(async () => {
      const goldPlan = fakePlanV2({ name: 'gold plan ⭐️' });
      await initComponent([goldPlan]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: /CLOSED/ })).then((btn) => btn.toggle());

      const closedPlan = fakePlanV2({ name: 'closed plan 🚪', status: 'CLOSED' });
      expectApiPlansListRequest([closedPlan], 'CLOSED');

      const { rowCells } = await computePlansTableCells();
      expect(rowCells).toEqual([['', 'closed plan 🚪', 'API_KEY', 'CLOSED', 'tag1', '']]);
    }));

    it('should search and not find any plan', fakeAsync(async () => {
      await initComponent([fakePlanV2()]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then((btn) => btn.toggle());

      expectApiPlansListRequest([], 'STAGING');

      const { rowCells } = await computePlansTableCells();
      expect(rowCells).toEqual([['There is no plan (yet).']]);
    }));
  });

  describe('drop tests', () => {
    it('should drop a plan and update his order', async () => {
      const plan1 = fakePlanV2({ name: 'Plan 1️⃣', order: 1 });
      const plan2 = fakePlanV2({ name: 'Plan 2️⃣', order: 2 });
      await initComponent([plan1, plan2]);

      component.dropRow({ previousIndex: 1, currentIndex: 0 } as any);

      expectApiPlanUpdateRequest({ ...plan2, order: 1 });
      expectApiGetRequest();
      expectApiPlansListRequest(
        [
          { ...plan2, order: 1 },
          { ...plan1, order: 2 },
        ],
        [...PLAN_STATUS],
      );
    });

    it('should fail to update and reload plans', async () => {
      const plan1 = fakePlanV2({ name: 'Plan 1️⃣', order: 1 });
      const plan2 = fakePlanV2({ name: 'Plan 2️⃣', order: 2 });
      await initComponent([plan1, plan2]);
      const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');

      component.dropRow({ previousIndex: 1, currentIndex: 0 } as any);

      expectApiPlanUpdateRequestFail({ ...plan2, order: 1 });
      expectApiGetRequest();
      expectApiPlansListRequest(
        [
          { ...plan2, order: 1 },
          { ...plan1, order: 2 },
        ],
        [...PLAN_STATUS],
      );
      expect(snackBarSpy).toHaveBeenCalled();
    });
  });

  describe('actions tests', () => {
    describe('with Angular router', () => {
      it('should navigate to edit when click on the action button', async () => {
        await init(fakeAjsGlobals);
        const plan = fakePlanV2();
        await initComponent([plan]);
        fakeUiRouter.go.mockReset();

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Edit the plan"]' })).then((btn) => btn.click());

        expect(fakeUiRouter.go).toBeCalledWith('management.apis.plan.edit', { planId: plan.id });
      });
      it('should navigate to new plan', async () => {
        await init(fakeAjsGlobals);
        await initComponent([]);
        fixture.componentInstance.isLoadingData = false;
        fakeUiRouter.go.mockReset();

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then((btn) => btn.click());

        const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
        expect(await planSecurityDropdown.getItems().then((items) => items.length)).toEqual(4);

        await planSecurityDropdown.clickItem({ text: 'Keyless (public)' });
        expect(fakeUiRouter.go).toBeCalledWith('management.apis.plan.new', { selectedPlanMenuItem: 'KEY_LESS' });
      });
      it('should navigate to edit when click on the name', async () => {
        await init(fakeAjsGlobals);
        const plan = fakePlanV2();
        await initComponent([plan]);
        fakeUiRouter.go.mockReset();

        await loader
          .getHarness(MatCellHarness.with({ text: plan.name }))
          .then((btn) => btn.host())
          .then((host) => host.click());

        expect(fakeUiRouter.go).toBeCalledWith('management.apis.plan.edit', { planId: plan.id });
      });
    });
    it('should navigate to edit when click on the action button', async () => {
      const plan = fakePlanV2();
      await initComponent([plan]);
      fakeUiRouter.go.mockReset();

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Edit the plan"]' })).then((btn) => btn.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.plan.edit', { planId: plan.id });
    });

    it('should navigate to new plan', async () => {
      await initComponent([]);
      fixture.componentInstance.isLoadingData = false;
      fakeUiRouter.go.mockReset();

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then((btn) => btn.click());

      const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
      expect(await planSecurityDropdown.getItems().then((items) => items.length)).toEqual(4);

      await planSecurityDropdown.clickItem({ text: 'Keyless (public)' });
      expect(fakeUiRouter.go).toBeCalledWith('management.apis.plan.new', { selectedPlanMenuItem: 'KEY_LESS' });
    });

    it('should navigate to edit when click on the name', async () => {
      const plan = fakePlanV2();
      await initComponent([plan]);
      fakeUiRouter.go.mockReset();

      await loader
        .getHarness(MatCellHarness.with({ text: plan.name }))
        .then((btn) => btn.host())
        .then((host) => host.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.plan.edit', { planId: plan.id });
    });

    it('should navigate to design when click on the design button', async () => {
      const plan = fakePlanV2();
      await initComponent([plan]);
      fakeUiRouter.go.mockReset();

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Design the plan"]' })).then((btn) => btn.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.policy-studio-v2.design', { apiId: API_ID, flows: `${plan.id}_0` });
    });

    describe('should publish the staging plan', () => {
      it('With a plan V2', async () => {
        const plan = fakePlanV2({ apiId: API_ID, name: 'publish me ☁️️', status: 'STAGING' });
        await initComponent([plan]);

        await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then((btn) => btn.toggle());
        expectApiPlansListRequest([plan], 'STAGING');

        const table = await computePlansTableCells();
        expect(table.rowCells).toEqual([['', 'publish me ☁️️', 'API_KEY', 'STAGING', 'tag1', '']]);

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' })).then((btn) => btn.click());

        const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
        const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
        await confirmDialogSwitchButton.click();

        const updatedPlan: Plan = { ...plan, status: 'PUBLISHED' };
        expectApiPlanPublishRequest(updatedPlan);
        expect(fakeRootScope.$broadcast).toHaveBeenCalledWith('apiChangeSuccess', { apiId: API_ID });
        expectApiGetRequest();
        expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
      });

      it('With a plan V4', async () => {
        const plan = fakePlanV4({ apiId: API_ID, name: 'publish me ☁️️', status: 'STAGING' });
        await initComponent([plan]);

        await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then((btn) => btn.toggle());
        expectApiPlansListRequest([plan], 'STAGING');

        const table = await computePlansTableCells();
        expect(table.rowCells).toEqual([['', 'publish me ☁️️', 'API_KEY', 'STAGING', 'tag1', '']]);

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' })).then((btn) => btn.click());

        const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
        const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
        await confirmDialogSwitchButton.click();

        const updatedPlan: Plan = { ...plan, status: 'PUBLISHED' };
        expectApiPlanPublishRequest(updatedPlan);
        expect(fakeRootScope.$broadcast).not.toHaveBeenCalled();
        expectApiGetRequest();
        expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
      });
    });

    describe('should deprecate the published plan V2', () => {
      it('With a plan V2', async () => {
        const plan = fakePlanV2({ apiId: API_ID, name: 'deprecate me 😥️', status: 'PUBLISHED' });
        await initComponent([plan]);

        const table = await computePlansTableCells();
        expect(table.rowCells).toEqual([['', 'deprecate me 😥️', 'API_KEY', 'PUBLISHED', 'tag1', '']]);

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Deprecate the plan"]' })).then((btn) => btn.click());

        const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#deprecatePlanDialog' }));
        const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Deprecate' }));
        await confirmDialogSwitchButton.click();

        const updatedPlan: Plan = { ...plan, status: 'DEPRECATED' };
        expectApiPlanDeprecateRequest(updatedPlan);
        expect(fakeRootScope.$broadcast).toHaveBeenCalledWith('apiChangeSuccess', { apiId: API_ID });
        expectApiGetRequest();
        expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
      });

      it('With a plan V4', async () => {
        const plan = fakePlanV4({ apiId: API_ID, name: 'deprecate me 😥️', status: 'PUBLISHED' });
        await initComponent([plan]);

        const table = await computePlansTableCells();
        expect(table.rowCells).toEqual([['', 'deprecate me 😥️', 'API_KEY', 'PUBLISHED', 'tag1', '']]);

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Deprecate the plan"]' })).then((btn) => btn.click());

        const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#deprecatePlanDialog' }));
        const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Deprecate' }));
        await confirmDialogSwitchButton.click();

        const updatedPlan: Plan = { ...plan, status: 'DEPRECATED' };
        expectApiPlanDeprecateRequest(updatedPlan);
        expect(fakeRootScope.$broadcast).not.toHaveBeenCalled();
        expectApiGetRequest();
        expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
      });
    });

    describe('close plan', () => {
      describe('should close the published plan', () => {
        it('With a plan V2', async () => {
          const plan = fakePlanV2({ apiId: API_ID, name: 'close me 🚪️', status: 'PUBLISHED' });
          await initComponent([plan]);

          const table = await computePlansTableCells();
          expect(table.rowCells).toEqual([['', 'close me 🚪️', 'API_KEY', 'PUBLISHED', 'tag1', '']]);

          await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' })).then((btn) => btn.click());

          expectGetApiPlanSubscriptionsRequest(plan.id);

          const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
          expect(await rootLoader.getHarness(MatButtonHarness.with({ text: 'Yes, close this plan.' }))).toBeTruthy();
          await confirmDialog.confirm();

          const updatedPlan: Plan = { ...plan, status: 'CLOSED' };
          expectApiPlanCloseRequest(updatedPlan);
          expect(fakeRootScope.$broadcast).toHaveBeenCalledWith('apiChangeSuccess', { apiId: API_ID });
          expectApiGetRequest();
          expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
        });

        it('With a plan V4', async () => {
          const plan = fakePlanV4({ apiId: API_ID, name: 'close me 🚪️', status: 'PUBLISHED' });
          await initComponent([plan]);

          const table = await computePlansTableCells();
          expect(table.rowCells).toEqual([['', 'close me 🚪️', 'API_KEY', 'PUBLISHED', 'tag1', '']]);

          await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' })).then((btn) => btn.click());

          expectGetApiPlanSubscriptionsRequest(plan.id);

          const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
          expect(await rootLoader.getHarness(MatButtonHarness.with({ text: 'Yes, close this plan.' }))).toBeTruthy();
          await confirmDialog.confirm();

          const updatedPlan: Plan = { ...plan, status: 'CLOSED' };
          expectApiPlanCloseRequest(updatedPlan);
          expect(fakeRootScope.$broadcast).not.toHaveBeenCalled();
          expectApiGetRequest();
          expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
        });
      });
    });
  });

  describe('kubernetes api tests', () => {
    it('should access plan in read only', async () => {
      const kubernetesApi = fakeApiV2({ id: API_ID, definitionContext: { origin: 'KUBERNETES' } });
      const plan = fakePlanV2({ apiId: API_ID, tags: ['tag1', 'tag2'] });
      await initComponent([plan], kubernetesApi);

      expect(component.isReadOnly).toBe(true);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="View the plan details"]' })).then((btn) => btn.click());

      expect(await loader.getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' }))).toHaveLength(0);
      expect(fakeUiRouter.go).toBeCalledWith('management.apis.plan.edit', { planId: plan.id });

      const { headerCells, rowCells } = await computePlansTableCells();
      expect(headerCells).toEqual([
        {
          name: 'Name',
          status: 'Status',
          'deploy-on': 'Deploy on',
          type: 'Type',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([['Default plan', 'API_KEY', 'PUBLISHED', 'tag1, tag2', '']]);
    });
  });

  describe('V1 API tests', () => {
    it('should access plan in read only', async () => {
      const v1Api = fakeApiV1({ id: API_ID });
      const plan = fakePlanV2({ apiId: API_ID, tags: ['tag1', 'tag2'] });
      await initComponent([plan], v1Api);

      expect(component.isReadOnly).toBe(true);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="View the plan details"]' })).then((btn) => btn.click());

      expect(await loader.getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' }))).toHaveLength(0);
      expect(fakeUiRouter.go).toBeCalledWith('management.apis.plan.edit', { planId: plan.id });

      const { headerCells, rowCells } = await computePlansTableCells();
      expect(headerCells).toEqual([
        {
          name: 'Name',
          status: 'Status',
          'deploy-on': 'Deploy on',
          type: 'Type',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([['Default plan', 'API_KEY', 'PUBLISHED', 'tag1, tag2', '']]);
    });
  });

  async function initComponent(plans: Plan[], api: Api = anAPi) {
    await TestBed.overrideProvider(UIRouterStateParams, { useValue: { apiId: api.id } }).compileComponents();
    fixture = TestBed.createComponent(ApiGeneralPlanListComponent);
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

  function expectApiPlansListRequest(plans: Plan[] = [], statuses?: string | string[], security?: string) {
    const response: ApiPlansResponse = { data: plans };
    httpTestingController
      .expectOne(
        `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=9999&${
          statuses ? `statuses=${castArray(statuses).join(',')}` : 'statuses=PUBLISHED'
        }${security ? `securities=${security}` : ''}`,
        'GET',
      )
      .flush(response);
    fixture.detectChanges();
  }

  function expectApiPlanUpdateRequest(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}`, 'PUT');
    expect(req.request.body).toEqual(plan);
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiPlanUpdateRequestFail(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}`, 'PUT');
    expect(req.request.body).toEqual(plan);
    req.error(new ErrorEvent('error'), { status: 400 });
    fixture.detectChanges();
  }

  function expectApiPlanPublishRequest(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}/_publish`, 'POST');
    expect(req.request.body).toEqual({});
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiPlanDeprecateRequest(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}/_deprecate`, 'POST');
    expect(req.request.body).toEqual({});
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiPlanCloseRequest(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}/_close`, 'POST');
    expect(req.request.body).toEqual({});
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiGetRequest(api: Api = anAPi) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
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
