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
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import moment from 'moment';
import { OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import { ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { ApiRuntimeLogsModule } from './api-runtime-logs.module';
import { ApiRuntimeLogsComponent } from './api-runtime-logs.component';
import { ApiRuntimeLogsHarness } from './api-runtime-logs.component.harness';
import { QuickFiltersStoreService } from './services';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import {
  ApiLogsParam,
  ApiV4,
  ConnectionLog,
  fakeApiLogsResponse,
  fakeApiV4,
  fakeConnectionLog,
  fakeEmptyApiLogsResponse,
  fakePagedResult,
  fakePlanV4,
  PlanV4,
} from '../../../../entities/management-api-v2';
import { fakeApplication } from '../../../../entities/application/Application.fixture';
import { Application } from '../../../../entities/application/application';

type ComponentInitData = {
  hasLogs: boolean;
  total?: number | undefined;
  perPage?: number | undefined;
  plans?: PlanV4[] | undefined;
  areLogsEnabled?: boolean;
};

describe('ApiRuntimeLogsComponent', () => {
  let fixture: ComponentFixture<ApiRuntimeLogsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiRuntimeLogsHarness;
  let routerNavigateSpy: jest.SpyInstance;

  const API_ID = 'an-api-id';
  const queryParams = { page: 1, perPage: 10 };
  const plan1 = fakePlanV4({ id: '1', name: 'plan 1' });
  const plan2 = fakePlanV4({ id: '2', name: 'plan 2' });
  const application = fakeApplication({ id: '1', owner: { displayName: 'owner' } });
  const fromDate = '2023-10-09 15:21:00';
  const fromDateTime = new Date(fromDate).getTime();
  const toDate = '2023-10-24 15:21:00';
  const toDateTime = new Date(toDate).getTime();

  const initComponent = async () => {
    TestBed.configureTestingModule({
      imports: [
        ApiRuntimeLogsModule,
        NoopAnimationsModule,
        MatIconTestingModule,
        GioTestingModule,
        OwlDateTimeModule,
        OwlMomentDateTimeModule,
      ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID }, queryParams: queryParams } } },
        QuickFiltersStoreService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsHarness);
    fixture.detectChanges();
  };

  const initComponentWithLogs = async ({ hasLogs, total, perPage, plans, areLogsEnabled = true }: ComponentInitData) => {
    await initComponent();
    expectPlanList(plans);
    hasLogs ? expectApiWithLogs(total) : expectApiWithNoLog();
    areLogsEnabled ? expectApiWithLogEnabled() : expectApiWithLogDisabled();
    expectRouterUrlChange(1, { page: 1, perPage: perPage ?? 10 });
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('When Loading', () => {
    it('should display load spinner', async () => {
      await initComponent();
      expectPlanList([]);

      expect(await componentHarness.loader()).not.toBeNull();

      expectApiWithNoLog();
      expectApiWithLogEnabled();

      expect(await componentHarness.loader()).toBeNull();
    });

    it('should disable', async () => {
      await initComponentWithLogs({ hasLogs: false });

      const quickFilter = await componentHarness.quickFiltersHarness();
      await quickFilter.clickRefresh();

      expect(await quickFilter.getPlansSelect().then((select) => select.isDisabled())).toBeTruthy();
      expect(await quickFilter.getRefreshButton().then((select) => select.isDisabled())).toBeTruthy();

      expectApiWithNoLog();
    });
  });

  describe('GIVEN there are no logs', () => {
    beforeEach(async () => {
      await initComponentWithLogs({ hasLogs: false });
    });

    it('should display the empty panel', async () => {
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeTruthy();
    });

    it('should not display the banner', async () => {
      expect(await componentHarness.isImpactBannerDisplayed()).toBeFalsy();
    });

    it('should navigate to logs settings', async () => {
      await componentHarness.clickOpenSettings();
      expect(routerNavigateSpy).toHaveBeenCalledWith(['../runtime-logs-settings'], {
        relativeTo: expect.anything(),
      });
    });

    it('should not display the list', async () => {
      expect(await componentHarness.getRows()).toHaveLength(0);
      expect(await componentHarness.getPaginator()).toBeNull();
    });
  });

  describe('GIVEN there are logs', () => {
    describe('when there is one page', () => {
      const total = 1;
      beforeEach(async () => {
        await initComponentWithLogs({ hasLogs: true, total: 1 });
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
      beforeEach(async () => {
        await initComponentWithLogs({ hasLogs: true, total });
      });

      it('should display the 1st page', async () => {
        expect(await componentHarness.getRows()).toHaveLength(perPage);
        const paginator = await componentHarness.getPaginator();
        expect(await paginator.getPageSize()).toBe(perPage);
        expect(await paginator.getRangeLabel()).toBe(`1 – ${perPage} of ${total}`);
      });

      it('should navigate to next page', async () => {
        await componentHarness.goToNextPage();
        expectSecondPage(total, perPage);
        expectRouterUrlChange(2, { page: 2, perPage: 10 });
      });

      it('should change page size', async () => {
        const paginator = await componentHarness.getPaginator();
        await paginator.setPageSize(25);
        expectApiWithLogs(total, { perPage: 25, page: 1 });
        expectRouterUrlChange(2, { page: 1, perPage: 25 });
      });
    });
  });

  describe('GIVEN there are logs with filters', () => {
    const total = 50;
    const perPage = 10;

    describe('when we arrive at default state', () => {
      const total = 1;
      beforeEach(async () => {
        await initComponentWithLogs({ hasLogs: true, total });
      });

      it('should display quick filters in default state', async () => {
        expect(await componentHarness.getRows()).toHaveLength(total);
        const periodSelectInput = await componentHarness.selectPeriodQuickFilter();
        expect(await periodSelectInput.isDisabled()).toEqual(false);
        expect(await periodSelectInput.getValueText()).toEqual('None');
      });
    });

    describe('when there is more than one page and we apply a period filter', () => {
      const fakeNow = moment('2023-10-05T00:00:00.000Z');
      const last5Min = 'Last 5 Minutes';

      beforeEach(async () => {
        await initComponentWithLogs({ hasLogs: true, total });

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
        expectRouterUrlChange(2, { page: 1, perPage: 10, from: expectedFrom, to: expectedTo });

        await componentHarness.goToNextPage();
        expectApiWithLogs(total, { perPage, page: 2, from: expectedFrom, to: expectedTo });
      });

      it('should navigate filter on last 5 minutes and remove it', async () => {
        const periodSelectInput = await componentHarness.selectPeriodQuickFilter();
        expect(await periodSelectInput.isDisabled()).toEqual(false);
        expect(await periodSelectInput.getValueText()).toEqual('None');
        await periodSelectInput.clickOptions({ text: last5Min });
        expect(await periodSelectInput.getValueText()).toEqual(last5Min);

        const expectedTo = fakeNow.valueOf();
        const expectedFrom = expectedTo - 5 * 60 * 1000;

        expectApiWithLogs(total, { perPage, page: 1, from: expectedFrom, to: expectedTo });

        // First time, add filters to URL
        expectRouterUrlChange(2, { page: 1, perPage: 10, from: expectedFrom, to: expectedTo });

        await componentHarness.removePeriodChip();
        expectApiWithLogs(total, { perPage, page: 1 });

        // Second time, we removed the filter from URL
        expectRouterUrlChange(3, { page: 1, perPage: 10 });

        expect(await periodSelectInput.isDisabled()).toEqual(false);
        expect(await periodSelectInput.getValueText()).toEqual('None');
        // We do not expect any chip since there is no filter
        expect(await componentHarness.getQuickFiltersChips()).toBeNull();
      });

      it('should sync period filters from quick filters and more filters', async () => {
        const quickFiltersPeriod = await componentHarness.selectPeriodQuickFilter();
        await quickFiltersPeriod.clickOptions({ text: last5Min });
        expect(await quickFiltersPeriod.getValueText()).toEqual(last5Min);
        expectApiWithLogs(total, { perPage, page: 1, from: fakeNow.valueOf() - 5 * 60 * 1000, to: fakeNow.valueOf() });

        await componentHarness.moreFiltersButtonClick();
        const moreFiltersPeriod = await componentHarness.selectPeriodFromMoreFilters();
        expect(await moreFiltersPeriod.getValueText()).toEqual(last5Min);
      });
    });

    describe('when there is more than one page and we apply a filter on applications', () => {
      beforeEach(async () => {
        await initComponentWithLogs({ hasLogs: true, total });
      });

      it('should filter on application', async () => {
        const appName = 'a';
        const application = fakeApplication({ name: appName, owner: { displayName: 'owner' } });

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getApplicationsTags()).toHaveLength(0);

        await componentHarness.searchApplication(appName);
        expectApplicationList(appName, [application]);

        await componentHarness.selectedApplication('a ( owner )');
        expect(await componentHarness.getApplicationsTags()).toHaveLength(1);

        await componentHarness.moreFiltersApply();

        expectApplicationList(); // call happening after selecting an option
        expectApiWithLogs(total, { perPage, page: 1, applicationIds: application.id });
        expectRouterUrlChange(2, { page: 1, perPage: 10, applicationIds: application.id });

        await componentHarness.goToNextPage();
        expectApiWithLogs(total, { perPage, page: 2, applicationIds: application.id });
        expectRouterUrlChange(3, { page: 2, perPage: 10, applicationIds: application.id });
      });

      it('should remove application filter', async () => {
        const appName = 'a';
        const application = fakeApplication({ name: appName, owner: { displayName: 'owner' } });

        await componentHarness.moreFiltersButtonClick();
        await componentHarness.searchApplication(appName);
        expectApplicationList(appName, [application]);

        await componentHarness.selectedApplication('a ( owner )');
        await componentHarness.moreFiltersApply();
        expectApplicationList(); // call happening after selecting an option
        expectApiWithLogs(total, { perPage, page: 1, applicationIds: application.id });
        expectRouterUrlChange(2, { page: 1, perPage: 10, applicationIds: application.id });

        await componentHarness.removeApplicationsChip();
        expect(await componentHarness.getQuickFiltersChips()).toBeNull();
        expectApiWithLogs(total, { perPage, page: 1 });
        expectRouterUrlChange(3, { page: 1, perPage: 10 });

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getApplicationsTags()).toHaveLength(0);
      });
    });

    describe('when there is more than one page and we apply a filter on plans', () => {
      beforeEach(async () => {
        await initComponentWithLogs({ hasLogs: true, total, plans: [plan1, plan2] });
      });

      it('should filter on plans', async () => {
        expect(await componentHarness.getSelectedPlans()).toEqual('');

        await componentHarness.selectPlan(plan1.name);
        expect(await componentHarness.getSelectedPlans()).toEqual(plan1.name);
        expectApiWithLogs(total, { perPage, page: 1, planIds: plan1.id });
        expectRouterUrlChange(2, { page: 1, perPage: 10, planIds: plan1.id });

        await componentHarness.selectPlan(plan2.name);
        expect(await componentHarness.getSelectedPlans()).toEqual(`${plan1.name}, ${plan2.name}`);
        expectApiWithLogs(total, { perPage, page: 1, planIds: `${plan1.id},${plan2.id}` });
        expectRouterUrlChange(3, { page: 1, perPage: 10, planIds: `${plan1.id},${plan2.id}` });

        await componentHarness.goToNextPage();
        expectApiWithLogs(total, { perPage, page: 2, planIds: `${plan1.id},${plan2.id}` });
        expectRouterUrlChange(4, { page: 2, perPage: 10, planIds: `${plan1.id},${plan2.id}` });
      });

      it('should remove plan filter', async () => {
        await componentHarness.selectPlan(plan1.name);
        expect(await componentHarness.getSelectedPlans()).toEqual(plan1.name);
        expectApiWithLogs(total, { perPage, page: 1, planIds: plan1.id });
        expectRouterUrlChange(2, { page: 1, perPage: 10, planIds: plan1.id });

        await componentHarness.removePlanChip();
        expect(await componentHarness.getQuickFiltersChips()).toBeNull();
        expectApiWithLogs(total, { perPage, page: 1 });
        expectRouterUrlChange(3, { page: 1, perPage: 10 });
      });
    });

    describe('when there is more than one page and we apply a filter on methods', () => {
      beforeEach(async () => {
        await initComponentWithLogs({ hasLogs: true, total });
      });

      it('should filter on http request methods', async () => {
        expect(await componentHarness.getSelectedMethods()).toEqual('');

        await componentHarness.selectMethod('GET');
        expect(await componentHarness.getSelectedMethods()).toEqual('GET');
        expectApiWithLogs(total, { perPage, page: 1, methods: 'GET' });
        expectRouterUrlChange(2, { page: 1, perPage: 10, methods: 'GET' });

        await componentHarness.selectMethod('POST');
        expect(await componentHarness.getSelectedMethods()).toEqual('GET, POST');
        expectApiWithLogs(total, { perPage, page: 1, methods: 'GET,POST' });
        expectRouterUrlChange(3, { page: 1, perPage: 10, methods: 'GET,POST' });

        await componentHarness.goToNextPage();
        expectApiWithLogs(total, { perPage, page: 2, methods: 'GET,POST' });
        expectRouterUrlChange(4, { page: 2, perPage: 10, methods: 'GET,POST' });
      });

      it('should remove methods filter', async () => {
        await componentHarness.selectMethod('GET');
        expect(await componentHarness.getSelectedMethods()).toEqual('GET');
        expectApiWithLogs(total, { perPage, page: 1, methods: 'GET' });
        expectRouterUrlChange(2, { page: 1, perPage: 10, methods: 'GET' });
        expect(await componentHarness.getMethodsChipText()).toStrictEqual('methods: GET');

        await componentHarness.removeMethodsChip();
        expect(await componentHarness.getQuickFiltersChips()).toBeNull();
        expectApiWithLogs(total, { perPage, page: 1 });
        expectRouterUrlChange(3, { page: 1, perPage: 10 });
      });
    });

    describe('when we click on more filters button', () => {
      const fakeNow = moment('2023-10-25T00:00:00.000Z');

      beforeEach(async () => {
        await initComponentWithLogs({ hasLogs: true });
        jest.spyOn(Date, 'now').mockReturnValue(new Date('2023-10-25T00:00:00.000Z').getTime());
      });

      it('should display more filters panel', async () => {
        expect.assertions(2);
        try {
          await componentHarness.moreFiltersHarness();
        } catch (e) {
          expect(e.message).toMatch(/Failed to find element/);
        }
        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.moreFiltersHarness()).toBeTruthy();
      });

      it('should clear all existing filters', async () => {
        await componentHarness.moreFiltersButtonClick();
        await componentHarness.setFromDate(fromDate);
        expect(await componentHarness.getFromDate()).toStrictEqual(fromDate);
        await componentHarness.moreFiltersApply();
        expectApiWithLogs(total, { perPage, page: 1, from: fromDateTime });

        await componentHarness.moreFiltersClearAll();
        expectApiWithLogs(total, { perPage, page: 1 });

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getFromDate()).toStrictEqual('');
      });

      it('should apply date filters from more filters', async () => {
        await componentHarness.moreFiltersButtonClick();

        await componentHarness.setFromDate(toDate);
        await componentHarness.setToDate(fromDate);
        expect(await componentHarness.isMoreFiltersApplyDisabled()).toBeTruthy();

        await componentHarness.setFromDate(fromDate);
        await componentHarness.setToDate(toDate);
        expect(await componentHarness.isMoreFiltersApplyDisabled()).toBeFalsy();
        await componentHarness.moreFiltersApply();

        expectApiWithLogs(total, { perPage, page: 1, from: fromDateTime, to: toDateTime });
        expectRouterUrlChange(2, { page: 1, perPage: 10, from: fromDateTime, to: toDateTime });
        expect(await componentHarness.getFromChipText()).toEqual(`from:${fromDate}`);
        expect(await componentHarness.getToChipText()).toEqual(`to:${toDate}`);

        await componentHarness.moreFiltersClearAll();
        expectApiWithLogs(total, { perPage, page: 1 });

        await componentHarness.moreFiltersButtonClick();

        const select = await componentHarness.selectPeriodFromMoreFilters();
        await select.clickOptions({ text: 'Last 5 Minutes' });
        await componentHarness.moreFiltersApply();
        expect(await componentHarness.selectPeriodQuickFilter().then((select) => select.getValueText())).toEqual('Last 5 Minutes');
        expectApiWithLogs(total, { perPage, page: 1, from: fakeNow.valueOf() - 5 * 60 * 1000, to: fakeNow.valueOf() });
      });

      it('should close more filters panel without applying filter values', async () => {
        await componentHarness.moreFiltersButtonClick();

        await componentHarness.selectPeriodInMoreFilters('Last 5 Minutes');
        expect(await componentHarness.moreFiltersPeriodText()).toEqual('Last 5 Minutes');

        await componentHarness.closeMoreFilters();
        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.moreFiltersPeriodText()).toEqual('None');
      });

      it('should reset period when from or two is modified and vice versa', async () => {
        await componentHarness.moreFiltersButtonClick();

        await componentHarness.setFromDate(fromDate);
        await componentHarness.setToDate(toDate);
        expect(await componentHarness.getFromInputValue()).toStrictEqual(fromDate);
        expect(await componentHarness.getToInputValue()).toStrictEqual(toDate);
        const select = await componentHarness.selectPeriodFromMoreFilters();
        expect(await select.getValueText()).toStrictEqual('None');

        await select.clickOptions({ text: 'Last 5 Minutes' });
        expect(await componentHarness.getFromInputValue()).toStrictEqual('');
        expect(await componentHarness.getToInputValue()).toStrictEqual('');

        await componentHarness.setFromDate(fromDate);
        expect(await select.getValueText()).toStrictEqual('None');

        await select.clickOptions({ text: 'Last 5 Minutes' });
        await componentHarness.setToDate(fromDate);
        expect(await select.getValueText()).toStrictEqual('None');
      });

      it('should reset filters when clicking on dedicated chip', async () => {
        await componentHarness.moreFiltersButtonClick();

        await componentHarness.selectPeriodInMoreFilters('Last 5 Minutes');
        await componentHarness.moreFiltersApply();
        expectApiWithLogs(total, { perPage, page: 1, from: fakeNow.valueOf() - 5 * 60 * 1000, to: fakeNow.valueOf() });
        expect(await componentHarness.getPeriodChipText()).toStrictEqual('period: Last 5 Minutes');

        await componentHarness.removePeriodChip();
        expectApiWithLogs(total, { perPage, page: 1 });

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.moreFiltersPeriodText()).toStrictEqual('None');

        await componentHarness.setFromDate(fromDate);
        await componentHarness.moreFiltersApply();
        expectApiWithLogs(total, { perPage, page: 1, from: fromDateTime });
        expect(await componentHarness.getFromChipText()).toEqual(`from:${fromDate}`);

        await componentHarness.removeFromChip();
        expectApiWithLogs(total, { perPage, page: 1 });

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getFromDate()).toStrictEqual('');

        await componentHarness.setToDate(toDate);
        await componentHarness.moreFiltersApply();
        expectApiWithLogs(total, { perPage, page: 1, to: toDateTime });
        expect(await componentHarness.getToChipText()).toEqual(`to:${toDate}`);

        await componentHarness.removeToChip();
        expectApiWithLogs(total, { perPage, page: 1 });

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getToDate()).toStrictEqual('');
      });

      it('should filter on http request status', async () => {
        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getStatusesInputChips()).toEqual([]);

        await componentHarness.addInputStatusesChip('200');
        expect(await componentHarness.getStatusesInputChips()).toEqual(['200']);

        await componentHarness.moreFiltersApply();
        expect(await componentHarness.getStatusChip()).toStrictEqual('statuses: 200');
        expectApiWithLogs(total, { perPage, page: 1, statuses: '200' });
        expectRouterUrlChange(2, { page: 1, perPage: 10, statuses: '200' });

        await componentHarness.moreFiltersButtonClick();
        await componentHarness.addInputStatusesChip('202');
        expect(await componentHarness.getStatusesInputChips()).toEqual(['200', '202']);

        await componentHarness.moreFiltersApply();
        expect(await componentHarness.getStatusChip()).toStrictEqual('statuses: 200, 202');
        expectApiWithLogs(total, { perPage, page: 1, statuses: '200,202' });
        expectRouterUrlChange(3, { page: 1, perPage: 10, statuses: '200,202' });

        await componentHarness.moreFiltersButtonClick();
        await componentHarness.removeInputStatusChip('200');
        expect(await componentHarness.getStatusesInputChips()).toEqual(['202']);

        await componentHarness.moreFiltersApply();
        expect(await componentHarness.getStatusChip()).toStrictEqual('statuses: 202');
        expectApiWithLogs(total, { perPage, page: 1, statuses: '202' });
        expectRouterUrlChange(4, { page: 1, perPage: 10, statuses: '202' });

        await componentHarness.goToNextPage();
        expectApiWithLogs(total, { perPage, page: 2, statuses: '202' });
        expectRouterUrlChange(5, { page: 2, perPage: 10, statuses: '202' });
      });

      it('should remove status filter', async () => {
        await componentHarness.moreFiltersButtonClick();
        await componentHarness.addInputStatusesChip('404');
        await componentHarness.moreFiltersApply();

        expectApiWithLogs(total, { perPage, page: 1, statuses: '404' });
        expectRouterUrlChange(2, { page: 1, perPage: 10, statuses: '404' });

        await componentHarness.removeStatusChip();
        expectApiWithLogs(total, { perPage, page: 1 });
        expectRouterUrlChange(3, { page: 1, perPage: 10 });
      });
    });
  });

  describe('GIVEN there are logs but logs are disabled', () => {
    beforeEach(async () => {
      await initComponentWithLogs({ hasLogs: true, total: 1, areLogsEnabled: false });
    });

    it('should display a banner', async () => {
      expect(await componentHarness.isImpactBannerDisplayed()).toBeTruthy();
    });

    it('should display the list', async () => {
      expect(await componentHarness.getRows()).toHaveLength(1);
    });

    it('should navigate to logs settings', async () => {
      await componentHarness.clickOpenSettings();
      expect(routerNavigateSpy).toHaveBeenCalledWith(['../runtime-logs-settings'], {
        relativeTo: expect.anything(),
      });
    });
  });

  describe('GIVEN there is filters in the url to initialize the form', () => {
    const anotherApplication = fakeApplication({ id: '2', name: 'another one', owner: { displayName: 'owner' } });

    describe('there are applications and plans in the url', () => {
      beforeEach(async () => {
        await TestBed.overrideProvider(ActivatedRoute, {
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: {
                ...queryParams,
                applicationIds: `${application.id},${anotherApplication.id}`,
                planIds: `${plan1.id},${plan2.id}`,
              },
            },
          },
        }).compileComponents();
        await initComponent();
        expectPlanList([plan1, plan2]);
        expectApplicationFindByIds([application, anotherApplication]);
        expectApiWithLogs(10, { page: 1, perPage: 10, applicationIds: '1,2', planIds: '1,2' });
        expectApiWithLogEnabled();
      });

      it('should init the form with filters preselected', async () => {
        const expectedApplicationChip = 'applications: Default application ( owner ), another one ( owner )';
        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getApplicationsTags()).toHaveLength(2);
        expect(await componentHarness.getApplicationsChipText()).toStrictEqual(expectedApplicationChip);
        await componentHarness.closeMoreFilters();

        expectApplicationFindById(application);
        // Here we simulate that this application is not returned by the default search because it does not belong to the response first page data.
        // Nevertheless, we should be able to display it in the chips and the tags
        expectApplicationFindById(anotherApplication);

        expect(await componentHarness.getSelectedPlans()).toEqual('plan 1, plan 2');
        expect(await componentHarness.getPlanChip()).toBeTruthy();

        await componentHarness.removePlanChip();
        // removing a chip should not impact on the application chip computed from the cache
        expect(await componentHarness.getApplicationsChipText()).toStrictEqual(expectedApplicationChip);
        expectApiWithLogs(10, { page: 1, perPage: 10, applicationIds: '1,2' });
      });

      it("should reset all filters when clicking on 'reset filters'", async () => {
        expect(await componentHarness.getApplicationsChip()).toBeTruthy();
        expect(await componentHarness.getPlanChip()).toBeTruthy();

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getApplicationsTags()).toHaveLength(2);
        await componentHarness.closeMoreFilters();
        expectApplicationFindById(application);
        expectApplicationFindById(anotherApplication);

        await componentHarness.quickFiltersHarness().then((harness) => harness.clickResetFilters());
        expect(await componentHarness.getSelectedPlans()).toEqual('');
        expectApiWithLogs(10, { page: 1, perPage: 10 });

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getApplicationsTags()).toHaveLength(0);

        await componentHarness.setFromDate(fromDate);
        await componentHarness.setToDate(toDate);
        await componentHarness.moreFiltersApply();
        expectApiWithLogs(10, { page: 1, perPage: 10, from: fromDateTime, to: toDateTime });

        await componentHarness.quickFiltersHarness().then((harness) => harness.clickResetFilters());
        expectApiWithLogs(10, { page: 1, perPage: 10 });

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getFromDate()).toStrictEqual('');
        expect(await componentHarness.getToDate()).toStrictEqual('');
      });
    });

    describe('there are from and to filters in the url', () => {
      beforeEach(async () => {
        await TestBed.overrideProvider(ActivatedRoute, {
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: {
                ...queryParams,
                from: fromDateTime,
                to: toDateTime,
              },
            },
          },
        }).compileComponents();

        await initComponent();
        expectPlanList();
        expectApiWithLogs(10, { page: 1, perPage: 10, from: fromDateTime, to: toDateTime });
        expectApiWithLogEnabled();
      });

      it('should init the form with filters preselected', async () => {
        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getFromInputValue()).toEqual(fromDate);
        expect(await componentHarness.getFromChipText()).toEqual(`from:${fromDate}`);
        expect(await componentHarness.getToInputValue()).toEqual(toDate);
        expect(await componentHarness.getToChipText()).toEqual(`to:${toDate}`);
      });
    });

    describe('there are methods filters in the url', () => {
      it('should init the form with filters preselected', async () => {
        await TestBed.overrideProvider(ActivatedRoute, {
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: {
                ...queryParams,
                methods: 'POST',
              },
            },
          },
        }).compileComponents();

        await initComponent();
        expectPlanList();
        expectApiWithLogs(10, { page: 1, perPage: 10, methods: 'POST' });
        expectApiWithLogEnabled();

        expect(await componentHarness.getSelectedMethods()).toEqual('POST');
        expect(await componentHarness.getMethodsChipText()).toStrictEqual('methods: POST');
      });
    });

    describe('should load page 2 by default', () => {
      beforeEach(async () => {
        await TestBed.overrideProvider(ActivatedRoute, {
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: {
                ...queryParams,
                page: 2,
              },
            },
          },
        }).compileComponents();
        await initComponent();
        expectPlanList();
      });

      it('should init the form with filters preselected', async () => {
        expectApiWithLogs(10, { page: 2, perPage: 10 });
        expectApiWithLogEnabled();
        expectRouterUrlChange(1, { page: 2, perPage: 10 });
      });
    });

    describe('there are status filters in the url', () => {
      it('should init the form with filters preselected', async () => {
        await TestBed.overrideProvider(ActivatedRoute, {
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: {
                ...queryParams,
                statuses: '200,202',
              },
            },
          },
        }).compileComponents();

        await initComponent();
        expectPlanList();
        expectApiWithLogs(10, { page: 1, perPage: 10, statuses: '200,202' });
        expectApiWithLogEnabled();
        expect(await componentHarness.getStatusChip()).toStrictEqual('statuses: 200, 202');

        await componentHarness.moreFiltersButtonClick();
        expect(await componentHarness.getStatusesInputChips()).toEqual(['200', '202']);
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
    fixture.detectChanges();
  }

  function expectApiWithLogs(total: number, param: ApiLogsParam = { perPage: 10, page: 1 }) {
    const itemsInPage = total < param.perPage ? total : param.perPage;

    const data: ConnectionLog[] = [];
    for (let i = 0; i < itemsInPage; i++) {
      data.push(fakeConnectionLog());
    }

    let expectedURL = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs?page=${param.page ?? 1}&perPage=${param.perPage ?? 10}`;
    if (param.from) expectedURL = expectedURL.concat(`&from=${param.from}`);
    if (param.to) expectedURL = expectedURL.concat(`&to=${param.to}`);
    if (param.applicationIds) expectedURL = expectedURL.concat(`&applicationIds=${param.applicationIds}`);
    if (param.planIds) expectedURL = expectedURL.concat(`&planIds=${param.planIds}`);
    if (param.methods) expectedURL = expectedURL.concat(`&methods=${param.methods}`);
    if (param.statuses) expectedURL = expectedURL.concat(`&statuses=${param.statuses}`);

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
      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=${applications.length}${applications
            .map((app) => `&ids=${app.id}`)
            .join('')}`,
          method: 'GET',
        })
        .flush(fakePagedResult(applications));
    } else {
      httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=10`,
        method: 'GET',
      });
    }
    fixture.detectChanges();
  }

  function expectApplicationFindById(application: Application) {
    const req = httpTestingController
      .match({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${application.id}`,
        method: 'GET',
      })
      .filter((req) => !req.cancelled);
    expect(req.length > 0).toBeTruthy();
    req[0].flush(application);
    fixture.detectChanges();
  }

  function expectPlanList(plans: PlanV4[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=9999&statuses=PUBLISHED,DEPRECATED,CLOSED`,
        method: 'GET',
      })
      .flush(fakePagedResult(plans));
    fixture.detectChanges();
  }

  function expectSecondPage(total: number, perPage = 10) {
    return expectApiWithLogs(total, { perPage, page: 2 });
  }

  function expectRouterUrlChange(nthCall: number, queryParams) {
    expect(routerNavigateSpy).toHaveBeenNthCalledWith(nthCall, ['.'], {
      relativeTo: expect.anything(),
      queryParams: { applicationIds: null, planIds: null, methods: null, statuses: null, from: null, to: null, ...queryParams },
    });
  }
});
