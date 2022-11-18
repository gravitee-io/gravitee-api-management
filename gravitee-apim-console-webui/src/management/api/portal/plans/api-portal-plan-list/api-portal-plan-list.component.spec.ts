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

import { ApiPortalPlanListComponent } from './api-portal-plan-list.component';

import { ApiPortalPlansModule } from '../api-portal-plans.module';
import { Api, ApiPlan } from '../../../../../entities/api';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { fakePlan } from '../../../../../entities/plan/plan.fixture';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { User as DeprecatedUser } from '../../../../../entities/user';

describe('ApiPortalPlanListComponent', () => {
  const API_ID = 'api#1';
  const anAPi = fakeApi({ id: API_ID });
  const fakeUiRouter = { go: jest.fn() };
  const currentUser = new DeprecatedUser();
  currentUser.userPermissions = ['api-plan-u', 'api-plan-r', 'api-plan-d'];

  let fixture: ComponentFixture<ApiPortalPlanListComponent>;
  let component: ApiPortalPlanListComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiPortalPlansModule, NoopAnimationsModule, GioHttpTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
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
      expect(rowCells).toEqual([['drag_indicator', 'Free Spaceshuttle', 'KEY_LESS', 'PUBLISHED', '🙅, 🔑', '']]);
    }));

    it('should search closed plan on click', fakeAsync(async () => {
      const goldPlan = fakePlan({ name: 'gold plan ⭐️' });
      await initComponent([goldPlan]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: 'CLOSED' })).then((btn) => btn.toggle());

      const closedPlan = fakePlan({ name: 'closed plan 🚪', status: 'CLOSED' });
      expectApiPlansListRequest([closedPlan], 'CLOSED');

      const { rowCells } = await computePlansTableCells();
      expect(rowCells).toEqual([['drag_indicator', 'closed plan 🚪', 'KEY_LESS', 'CLOSED', '', '']]);
    }));

    it('should search and not find any plan', fakeAsync(async () => {
      await initComponent([fakePlan()]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: 'STAGING' })).then((btn) => btn.toggle());

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

      expectApiPlansPutRequest({ ...plan2, order: 1 });
      expectApiGetRequest(anAPi);
      expectApiPlansListRequest([
        { ...plan2, order: 1 },
        { ...plan1, order: 2 },
      ]);
    });
  });

  describe('actions tests', () => {
    it('should navigate to edit when click on the action button', async () => {
      const plan = fakePlan();
      await initComponent([plan]);
      fakeUiRouter.go.mockReset();

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Edit the plan"]' })).then((btn) => btn.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.detail.portal.plans.plan', { planId: plan.id });
    });

    it('should navigate to edit when click on the name', async () => {
      const plan = fakePlan();
      await initComponent([plan]);
      fakeUiRouter.go.mockReset();

      await loader
        .getHarness(MatCellHarness.with({ text: plan.name }))
        .then((btn) => btn.host())
        .then((host) => host.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.detail.portal.plans.plan', { planId: plan.id });
    });

    it('should navigate to design when click on the design button', async () => {
      const plan = fakePlan();
      await initComponent([plan]);
      fakeUiRouter.go.mockReset();

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Design the plan"]' })).then((btn) => btn.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.detail.design.flowsNg', { apiId: API_ID, flows: `${plan.id}_0` });
    });
  });

  describe('kubernetes api tests', () => {
    it('should access plan in read only', async () => {
      const kubernetesApi = fakeApi({ id: API_ID, definition_context: { origin: 'kubernetes' } });
      const plan = fakePlan({ api: API_ID });
      await initComponent([plan], kubernetesApi);

      expect(component.isReadOnly).toBe(true);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="View the plan details"]' })).then((btn) => btn.click());

      expect(fakeUiRouter.go).toBeCalledWith('management.apis.detail.portal.plans.plan', { planId: plan.id });
    });
  });

  async function initComponent(plans: ApiPlan[], api: Api = anAPi) {
    await TestBed.overrideProvider(UIRouterStateParams, { useValue: { apiId: api.id } }).compileComponents();
    fixture = TestBed.createComponent(ApiPortalPlanListComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    expectApiPlansListRequest(plans);
    expectApiGetRequest(api);

    loader = TestbedHarnessEnvironment.loader(fixture);
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

  function expectApiPlansListRequest(plans: ApiPlan[] = [], status?: string, security?: string) {
    httpTestingController
      .expectOne(
        `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans?${status ? `status=${status}` : 'status=PUBLISHED'}${
          security ? `security=${security}` : ''
        }`,
        'GET',
      )
      .flush(plans);
    fixture.detectChanges();
  }

  function expectApiPlansPutRequest(plan: ApiPlan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans/${plan.id}`, 'PUT');
    expect(req.request.body).toEqual(plan);
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
