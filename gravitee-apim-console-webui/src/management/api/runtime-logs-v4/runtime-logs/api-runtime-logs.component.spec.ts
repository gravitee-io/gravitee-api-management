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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { UIRouterModule } from '@uirouter/angular';
import * as moment from 'moment';

import { ApiRuntimeLogsModule } from './api-runtime-logs.module';
import { ApiRuntimeLogsComponent } from './api-runtime-logs.component';
import { ApiRuntimeLogsHarness } from './api-runtime-logs.component.harness';
import { ApiRuntimeLogsListRowHarness } from './components';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ApiLogsParam, ApiV4, ConnectionLog, fakeApiV4, fakePagedResult, fakePlanV4, PlanV4 } from '../../../../entities/management-api-v2';
import { fakeApiLogsResponse, fakeEmptyApiLogsResponse } from '../../../../entities/management-api-v2/log/apiLogsResponse.fixture';
import { fakeConnectionLog } from '../../../../entities/management-api-v2/log/connectionLog.fixture';
import { fakeApplication } from '../../../../entities/application/Application.fixture';
import { Application } from '../../../../entities/application/application';

describe('ApiRuntimeLogsComponent', () => {
  const API_ID = 'an-api-id';

  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiRuntimeLogsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiRuntimeLogsHarness;
  let logsRowHarness: ApiRuntimeLogsListRowHarness;
  const stateParams = { apiId: API_ID, page: 1, perPage: 10 };
  const plan1 = fakePlanV4({ id: '1', name: 'plan 1' });
  const plan2 = fakePlanV4({ id: '2', name: 'plan 2' });

  const initComponent = async () => {
    TestBed.configureTestingModule({
      imports: [
        ApiRuntimeLogsModule,
        NoopAnimationsModule,
        HttpClientTestingModule,
        MatIconTestingModule,
        GioHttpTestingModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
      ],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: UIRouterStateParams, useValue: stateParams },
      ],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsHarness);
    logsRowHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsListRowHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('GIVEN there are no logs', () => {
    beforeEach(async () => {
      await initComponent();
      expectApiWithNoLog();
      expectPlanList();
      expectApiWithLogEnabled();
      expectApplicationList();
    });

    it('should display the empty panel', async () => {
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeTruthy();
    });

    it('should not display the banner', async () => {
      expect(await componentHarness.isImpactBannerDisplayed()).toBeFalsy();
    });

    it('should navigate to logs settings', async () => {
      await componentHarness.clickOpenSettings();
      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.runtimeLogs-settings');
    });

    it('should not display the list', async () => {
      expect(await componentHarness.getRows()).toHaveLength(0);
      expect(await componentHarness.getPaginator()).toBeNull();
    });
  });

  describe('GIVEN there are logs', () => {
    beforeEach(async () => {
      await initComponent();
    });

    describe('when there is one page', () => {
      const total = 1;
      beforeEach(() => {
        expectApiWithLogs(total);
        expectPlanList();
        expectApiWithLogEnabled();
        expectApplicationList();
      });

      it('should display the 1st page with default pagination', async () => {
        expect(await componentHarness.getRows()).toHaveLength(total);

        const paginator = await componentHarness.getPaginator();
        expect(await paginator.getPageSize()).toBe(10);
      });
    });

    describe('when there is more than one page', () => {
      const total = 50;
      const perPage = 10;
      beforeEach(() => {
        expectApiWithLogs(total);
        expectPlanList();
        expectApiWithLogEnabled();
        expectApplicationList();
      });

      it('should display the 1st page', async () => {
        expect(await componentHarness.getRows()).toHaveLength(perPage);

        const paginator = await componentHarness.getPaginator();
        expect(await paginator.getPageSize()).toBe(perPage);
        expect(await paginator.getRangeLabel()).toBe(`1 – ${perPage} of ${total}`);
      });

      it('should navigate to next page', async () => {
        const paginator = await componentHarness.getPaginator();
        await paginator.goToNextPage();

        expectSecondPage(total, perPage);
        expect(fakeUiRouter.go).toHaveBeenCalledWith(
          '.',
          {
            page: 2,
            perPage: 10,
          },
          { notify: false },
        );
      });

      it('should change page size', async () => {
        const paginator = await componentHarness.getPaginator();
        await paginator.setPageSize(25);

        expectApiWithLogs(total, { perPage: 25, page: 1 });
        expect(fakeUiRouter.go).toHaveBeenCalledWith(
          '.',
          {
            page: 1,
            perPage: 25,
          },
          { notify: false },
        );
      });
    });
  });

  describe('GIVEN there are logs with filters', () => {
    beforeEach(async () => {
      await initComponent();
    });

    describe('when we arrive at default state', () => {
      const total = 1;
      beforeEach(() => {
        expectApiWithLogs(total);
        expectPlanList();
        expectApiWithLogEnabled();
        expectApplicationList();
      });

      it('should display quick filters in default state', async () => {
        expect(await componentHarness.getRows()).toHaveLength(total);
        const periodSelectInput = await componentHarness.selectPeriodQuickFilter();
        expect(await periodSelectInput.isDisabled()).toEqual(false);
        expect(await periodSelectInput.getValueText()).toEqual('None');
      });
    });

    describe('when there is more than one page and we apply a period filter', () => {
      const total = 50;
      const perPage = 10;
      const fakeNow = moment('2023-10-05T00:00:00.000Z');

      beforeEach(() => {
        expectApiWithLogs(total);
        expectPlanList();
        expectApiWithLogEnabled();
        expectApplicationList();

        // moment() is relying on Date.now, so fix it to be able to assert on from and to filters
        jest.spyOn(Date, 'now').mockReturnValue(new Date('2023-10-05T00:00:00.000Z').getTime());
      });

      it('should display the 1st page with default filter', async () => {
        expect(await componentHarness.getRows()).toHaveLength(perPage);

        const paginator = await componentHarness.getPaginator();
        expect(await paginator.getPageSize()).toBe(perPage);
        expect(await paginator.getRangeLabel()).toBe(`1 – ${perPage} of ${total}`);

        const periodSelectInput = await componentHarness.selectPeriodQuickFilter();
        expect(await periodSelectInput.isDisabled()).toEqual(false);
        expect(await periodSelectInput.getValueText()).toEqual('None');
      });

      it('should navigate to second page and keep the period filter', async () => {
        expect(await componentHarness.getRows()).toHaveLength(perPage);

        const periodSelectInput = await componentHarness.selectPeriodQuickFilter();
        await periodSelectInput.clickOptions({ text: 'Last 5 Minutes' });

        const expectedTo = fakeNow.valueOf();
        const expectedFrom = expectedTo - 5 * 60 * 1000;
        expectApiWithLogs(total, { perPage, page: 1, from: expectedFrom, to: expectedTo });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          1,
          '.',
          {
            page: 1,
            perPage: 10,
            from: expectedFrom,
            to: expectedTo,
            applicationIds: null,
            planIds: null,
          },
          { notify: false },
        );

        const paginator = await componentHarness.getPaginator();
        await paginator.goToNextPage();

        expectApiWithLogs(total, { perPage, page: 2, from: expectedFrom, to: expectedTo });
      });

      it('should navigate filter on last 5 minutes and remove it', async () => {
        const periodSelectInput = await componentHarness.selectPeriodQuickFilter();
        expect(await periodSelectInput.isDisabled()).toEqual(false);
        expect(await periodSelectInput.getValueText()).toEqual('None');
        await periodSelectInput.clickOptions({ text: 'Last 5 Minutes' });
        expect(await periodSelectInput.getValueText()).toEqual('Last 5 Minutes');

        const expectedTo = fakeNow.valueOf();
        const expectedFrom = expectedTo - 5 * 60 * 1000;

        expectApiWithLogs(total, { perPage, page: 1, from: expectedFrom, to: expectedTo });

        // First time, add filters to URL
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          1,
          '.',
          {
            page: 1,
            perPage: 10,
            from: expectedFrom,
            to: expectedTo,
            applicationIds: null,
            planIds: null,
          },
          { notify: false },
        );

        const periodChip = await componentHarness.getPeriodChip();
        const periodChipRemoveButton = await periodChip.getRemoveButton();
        await periodChipRemoveButton.click();

        expectApiWithLogs(total, { perPage, page: 1 });

        // Second time, we removed the filter from URL
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          2,
          '.',
          { page: 1, perPage: 10, from: null, to: null, applicationIds: null, planIds: null },
          { notify: false },
        );

        expect(await periodSelectInput.isDisabled()).toEqual(false);
        expect(await periodSelectInput.getValueText()).toEqual('None');
        // We do not expect any chip since there is no filter
        expect(await componentHarness.getQuickFiltersChips()).toBeNull();
      });
    });

    describe('when there is more than one page and we apply a filter on applications', () => {
      const total = 50;
      const perPage = 10;

      beforeEach(() => {
        expectApiWithLogs(total);
        expectPlanList();
        expectApiWithLogEnabled();
        expectApplicationList();
      });

      it('should filter on application', async () => {
        const appName = 'a';
        const application = fakeApplication({ name: appName, owner: { displayName: 'owner' } });
        expect(await componentHarness.getApplicationsTags()).toHaveLength(0);

        await componentHarness.searchApplication(appName);
        expectApplicationList(appName, [application]);

        await componentHarness.selectedApplication('a ( owner )');
        expect(await componentHarness.getApplicationsTags()).toHaveLength(1);

        expectApiWithLogs(total, { perPage, page: 1, applicationIds: application.id });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          1,
          '.',
          { page: 1, perPage: 10, applicationIds: application.id, from: null, to: null, planIds: null },
          { notify: false },
        );

        const paginator = await componentHarness.getPaginator();
        await paginator.goToNextPage();
        expectApiWithLogs(total, { perPage, page: 2, applicationIds: application.id });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          2,
          '.',
          { page: 2, perPage: 10, applicationIds: application.id },
          { notify: false },
        );
      });

      it('should remove application filter', async () => {
        const appName = 'a';
        const application = fakeApplication({ name: appName, owner: { displayName: 'owner' } });

        await componentHarness.searchApplication(appName);
        expectApplicationList(appName, [application]);
        fixture.detectChanges();

        await componentHarness.selectedApplication('a ( owner )');
        fixture.detectChanges();
        await fixture.whenStable();
        expectApiWithLogs(total, { perPage, page: 1, applicationIds: application.id });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          1,
          '.',
          { page: 1, perPage: 10, applicationIds: application.id, from: null, to: null, planIds: null },
          { notify: false },
        );

        await componentHarness
          .getApplicationsChip()
          .then((chip) => chip.getRemoveButton())
          .then((button) => button.click());

        expect(await componentHarness.getQuickFiltersChips()).toBeNull();
        expectApiWithLogs(total, { perPage, page: 1 });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          2,
          '.',
          { page: 1, perPage: 10, applicationIds: null, from: null, to: null, planIds: null },
          { notify: false },
        );
      });
    });

    describe('when there is more than one page and we apply a filter on plans', () => {
      const total = 50;
      const perPage = 10;

      beforeEach(() => {
        expectApiWithLogs(total);
        expectPlanList([plan1, plan2]);
        expectApiWithLogEnabled();
        expectApplicationList();
      });

      it('should filter on plans', async () => {
        expect(await componentHarness.getSelectedPlans()).toEqual('');

        await componentHarness.selectPlan(plan1.name);
        expect(await componentHarness.getSelectedPlans()).toEqual(plan1.name);
        expectApiWithLogs(total, { perPage, page: 1, planIds: plan1.id });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          1,
          '.',
          { page: 1, perPage: 10, planIds: plan1.id, applicationIds: null, from: null, to: null },
          { notify: false },
        );

        await componentHarness.selectPlan(plan2.name);
        expect(await componentHarness.getSelectedPlans()).toEqual(`${plan1.name}, ${plan2.name}`);
        expectApiWithLogs(total, { perPage, page: 1, planIds: `${plan1.id},${plan2.id}` });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          2,
          '.',
          { page: 1, perPage: 10, planIds: `${plan1.id},${plan2.id}`, applicationIds: null, from: null, to: null },
          { notify: false },
        );

        const paginator = await componentHarness.getPaginator();
        await paginator.goToNextPage();
        expectApiWithLogs(total, { perPage, page: 2, planIds: `${plan1.id},${plan2.id}` });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          3,
          '.',
          { page: 2, perPage: 10, planIds: `${plan1.id},${plan2.id}` },
          { notify: false },
        );
      });

      it('should remove plan filter', async () => {
        await componentHarness.selectPlan(plan1.name);
        expect(await componentHarness.getSelectedPlans()).toEqual(plan1.name);
        expectApiWithLogs(total, { perPage, page: 1, planIds: plan1.id });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          1,
          '.',
          { page: 1, perPage: 10, planIds: plan1.id, applicationIds: null, from: null, to: null },
          { notify: false },
        );

        await componentHarness
          .getPlanChip()
          .then((chip) => chip.getRemoveButton())
          .then((button) => button.click());

        expect(await componentHarness.getQuickFiltersChips()).toBeNull();
        expectApiWithLogs(total, { perPage, page: 1 });
        expect(fakeUiRouter.go).toHaveBeenNthCalledWith(
          2,
          '.',
          { page: 1, perPage: 10, applicationIds: null, planIds: null, from: null, to: null },
          { notify: false },
        );
      });
    });
  });

  describe('GIVEN there are logs but logs are disabled', () => {
    beforeEach(async () => {
      await initComponent();
      expectApiWithLogs(1);
      expectApiWithLogDisabled();
      expectPlanList();
      expectApplicationList();
    });

    it('should display a banner', async () => {
      expect(await componentHarness.isImpactBannerDisplayed()).toBeTruthy();
    });

    it('should display the list', async () => {
      expect(await componentHarness.getRows()).toHaveLength(1);
    });

    it('should navigate to logs settings', async () => {
      await componentHarness.clickOpenSettings();
      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.runtimeLogs-settings');
    });
  });

  describe('GIVEN the API is a proxy API', () => {
    it('should not display view message button', async () => {
      expect.assertions(1);

      await initComponent();
      expectApiWithNoLog();
      expectPlanList();
      expectApiWithLogEnabled({ type: 'PROXY' });
      expectApplicationList();

      try {
        await logsRowHarness.getViewMessageButton();
      } catch (e) {
        expect(e.message).toMatch(/Failed to find element/);
      }
    });
  });

  describe('GIVEN there is filters in the url to initialize the form', () => {
    const application = fakeApplication({ id: '1', owner: { displayName: 'owner' } });

    describe('there are applications and plans in the url', () => {
      beforeEach(async () => {
        await TestBed.overrideProvider(UIRouterStateParams, {
          useValue: { ...stateParams, applicationIds: application.id, planIds: `${plan1.id},${plan2.id}` },
        }).compileComponents();
        await initComponent();
        expectPlanList([plan1, plan2]);
        expectApiWithLogs(10, { page: 1, perPage: 10, applicationIds: '1', planIds: '1,2' });
        expectApiWithLogEnabled();
      });

      it('should init the form with filters preselected', async () => {
        expectApplicationFindByIds([application]);
        expectApplicationList();

        expect(await componentHarness.getApplicationsTags()).toHaveLength(1);
        expect(await componentHarness.getApplicationsChip()).toBeTruthy();
        expectApplicationFindById(application);

        expect(await componentHarness.getSelectedPlans()).toEqual('plan 1, plan 2');
        expect(await componentHarness.getPlanChip()).toBeTruthy();
      });
    });
  });

  function expectApiWithLogEnabled(modifier?: Partial<ApiV4>) {
    let api = fakeApiV4({ id: API_ID, analytics: { enabled: true, logging: { mode: { entrypoint: true } } } });

    if (modifier) {
      api = { ...api, ...modifier };
    }

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectApiWithLogDisabled() {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
        method: 'GET',
      })
      .flush(fakeApiV4({ id: API_ID, analytics: { enabled: true, logging: {} } }));
  }

  function expectApiWithNoLog() {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs?page=1&perPage=10`,
        method: 'GET',
      })
      .flush(fakeEmptyApiLogsResponse());
  }

  function expectApiWithLogs(total: number, param: ApiLogsParam = { perPage: 10, page: 1 }) {
    const itemsInPage = total < param.perPage ? total : param.perPage;

    const data: ConnectionLog[] = [];
    for (let i = 0; i < itemsInPage; i++) {
      data.push(fakeConnectionLog());
    }

    let expectedURL = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs?page=${param.page ?? 1}&perPage=${param.perPage ?? 10}`;
    if (param.from) {
      expectedURL = expectedURL.concat(`&from=${param.from}`);
    }
    if (param.to) {
      expectedURL = expectedURL.concat(`&to=${param.to}`);
    }

    if (param.applicationIds) {
      expectedURL = expectedURL.concat(`&applicationIds=${param.applicationIds}`);
    }

    if (param.planIds) {
      expectedURL = expectedURL.concat(`&planIds=${param.planIds}`);
    }

    httpTestingController
      .expectOne({
        url: expectedURL,
        method: 'GET',
      })
      .flush(
        fakeApiLogsResponse({
          data,
          pagination: {
            totalCount: total,
            page: param.page,
            perPage: param.perPage,
          },
        }),
      );
    fixture.detectChanges();
  }

  function expectApplicationList(searchTerm?: string, applications?: Application[]) {
    if (searchTerm) {
      const req = httpTestingController
        .match({
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=10&query=${searchTerm}`,
          method: 'GET',
        })
        .filter((req) => !req.cancelled);
      expect(req.length).toEqual(1);
      req[0].flush(fakePagedResult(applications));
    } else {
      httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=10`,
        method: 'GET',
      });
    }
    fixture.detectChanges();
  }

  function expectApplicationFindByIds(applications: Application[] = []) {
    if (applications.length > 0) {
      const req = httpTestingController
        .match({
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=${applications.length}&ids=${applications
            .map((app) => app.id)
            .join(',')}`,
          method: 'GET',
        })
        .filter((req) => !req.cancelled);
      expect(req.length).toEqual(1);
      req[0].flush(fakePagedResult(applications));
    } else {
      httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=10`,
        method: 'GET',
      });
    }
    fixture.detectChanges();
  }

  function expectApplicationFindById(application: Application) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${application.id}`,
        method: 'GET',
      })
      .flush(application);
    fixture.detectChanges();
  }

  function expectPlanList(plans: PlanV4[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=9999`,
        method: 'GET',
      })
      .flush(fakePagedResult(plans));
    fixture.detectChanges();
  }

  function expectSecondPage(total: number, perPage = 10) {
    return expectApiWithLogs(total, { perPage, page: 2 });
  }
});
