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
import { HttpTestingController } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatChipHarness } from '@angular/material/chips/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { ApplicationTabLogsComponent } from './application-tab-logs.component';
import { MoreFiltersDialogComponent } from './components/more-filters-dialog/more-filters-dialog.component';
import { MoreFiltersDialogHarness } from './components/more-filters-dialog/more-filters-dialog.harness';
import { fakeApplication } from '../../../../entities/application/application.fixture';
import { LogsResponse } from '../../../../entities/log/log';
import { fakeLog, fakeLogsResponse } from '../../../../entities/log/log.fixture';
import { fakeSubscription, fakeSubscriptionResponse } from '../../../../entities/subscription/subscription.fixture';
import { SubscriptionsResponse } from '../../../../entities/subscription/subscriptions-response';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

/* eslint-disable no-useless-escape */

describe('ApplicationTabLogsComponent', () => {
  let component: ApplicationTabLogsComponent;
  let fixture: ComponentFixture<ApplicationTabLogsComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;
  let rootHarnessLoader: HarnessLoader;

  const APP_ID = 'app-id';
  const MOCK_DATE = new Date(1466424490000);

  const init = async (queryParams: unknown) => {
    const asBehaviorSubject = new BehaviorSubject(queryParams);

    await TestBed.configureTestingModule({
      imports: [ApplicationTabLogsComponent, MoreFiltersDialogComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { queryParams: asBehaviorSubject.asObservable() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationTabLogsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    component = fixture.componentInstance;
    component.application = fakeApplication({ id: APP_ID });

    const router = TestBed.inject(Router);
    jest.spyOn(router, 'navigate').mockImplementation((_, configuration) => {
      asBehaviorSubject.next(configuration?.queryParams ?? {});
      return new Promise(_ => true);
    });

    jest.useFakeTimers({ advanceTimers: true }).setSystemTime(MOCK_DATE);

    fixture.detectChanges();
  };

  afterAll(async () => {
    jest.useRealTimers();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('No page in query params', () => {
    beforeEach(async () => {
      await init({});
    });

    it('should show empty message when no logs', async () => {
      expectGetApplicationLogs(fakeLogsResponse({ data: [], metadata: { data: { total: 0 } } }));
      expectGetSubscriptions(fakeSubscriptionResponse());
      fixture.detectChanges();

      expect(getNoLogsMessageSection()).toBeTruthy();
    });

    it('should show log data', async () => {
      expectGetApplicationLogs(
        fakeLogsResponse({
          data: [
            fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
            fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
          ],
          metadata: {
            'my-api': { name: 'My API', version: '1.0' },
            'my-api-2': { name: 'My API 2', version: '2.0' },
            'my-plan': { name: 'My Plan' },
            data: { total: 3 },
          },
        }),
      );
      expectGetSubscriptions(fakeSubscriptionResponse());

      fixture.detectChanges();

      expect(getNoLogsMessageSection()).toBeFalsy();

      const apiCellRowOne = await getTextByRowIndexAndColumnName(0, 'api');
      expect(apiCellRowOne).toContain('My API');
      expect(apiCellRowOne).toContain('Version: 1.0');

      expect(await getTextByRowIndexAndColumnName(0, 'timestamp')).toContain('2016-06-20');
      expect(await getTextByRowIndexAndColumnName(0, 'responseStatus')).toEqual('201');
      expect(await getTextByRowIndexAndColumnName(0, 'httpMethod')).toEqual('GET');

      const apiCellRowTwo = await getTextByRowIndexAndColumnName(1, 'api');
      expect(apiCellRowTwo).toContain('My API 2');
      expect(apiCellRowTwo).toContain('Version: 2.0');

      expect(await getTextByRowIndexAndColumnName(1, 'timestamp')).toContain('2019-08-21');
      expect(await getTextByRowIndexAndColumnName(1, 'responseStatus')).toEqual('204');
      expect(await getTextByRowIndexAndColumnName(1, 'httpMethod')).toEqual('GET');
    });

    describe('Pagination', () => {
      describe('Only one page of results', () => {
        beforeEach(async () => {
          expectGetApplicationLogs(
            fakeLogsResponse({
              data: [
                fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
                fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
              ],
              metadata: {
                'my-api': { name: 'My API', version: '1.0' },
                'my-api-2': { name: 'My API 2', version: '2.0' },
                'my-plan': { name: 'My Plan' },
                data: { total: 3 },
              },
            }),
          );
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });
        it('should not allow previous page on load', async () => {
          const previousPageButton = await getPreviousPageButton();
          expect(previousPageButton).toBeTruthy();
          expect(await previousPageButton.isDisabled()).toEqual(true);
        });
        it('should not allow next page when on last page', async () => {
          const nextPageButton = await getNextPageButton();
          expect(nextPageButton).toBeTruthy();
          expect(await nextPageButton.isDisabled()).toEqual(true);
        });
        it('should highlight current page', async () => {
          const currentPaginationPage = await getCurrentPaginationPage();
          expect(currentPaginationPage).toBeTruthy();
          expect(await currentPaginationPage.getText()).toEqual('1');
        });
      });
      describe('First of many pages of results', () => {
        beforeEach(async () => {
          expectGetApplicationLogs(
            fakeLogsResponse({
              data: [
                fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
                fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
              ],
              metadata: {
                'my-api': { name: 'My API', version: '1.0' },
                'my-api-2': { name: 'My API 2', version: '2.0' },
                'my-plan': { name: 'My Plan' },
                data: { total: 79 },
              },
            }),
          );
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });
        it('should not allow previous page on load', async () => {
          const previousPageButton = await getPreviousPageButton();
          expect(previousPageButton).toBeTruthy();
          expect(await previousPageButton.isDisabled()).toEqual(true);
        });
        it('should allow next page', async () => {
          const nextPageButton = await getNextPageButton();
          expect(nextPageButton).toBeTruthy();
          expect(await nextPageButton.isDisabled()).toEqual(false);
        });
        it('should show "2" for next page', async () => {
          const secondPageButton = await getPageButtonByLabel('2');
          expect(secondPageButton).toBeTruthy();
          expect(await secondPageButton.isDisabled()).toEqual(false);
        });
        it('should show "3" for page option', async () => {
          const thirdPageButton = await getPageButtonByLabel('3');
          expect(thirdPageButton).toBeTruthy();
          expect(await thirdPageButton.isDisabled()).toEqual(false);
        });

        it('should show "8" for last page', async () => {
          const lastPageButton = await getPageButtonByLabel('8');
          expect(lastPageButton).toBeTruthy();
          expect(await lastPageButton.isDisabled()).toEqual(false);
        });

        it('should highlight current page', async () => {
          const currentPaginationPage = await getCurrentPaginationPage();
          expect(currentPaginationPage).toBeTruthy();
          expect(await currentPaginationPage.getText()).toEqual('1');
        });
        it('should go to next page via page number button', async () => {
          const secondPageButton = await getPageButtonByLabel('2');
          await secondPageButton.click();
          fixture.detectChanges();

          expectGetApplicationLogs(
            fakeLogsResponse({
              data: [
                fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
                fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
              ],
              metadata: {
                'my-api': { name: 'My API', version: '1.0' },
                'my-api-2': { name: 'My API 2', version: '2.0' },
                'my-plan': { name: 'My Plan' },
                data: { total: 79 },
              },
            }),
            2,
          );
        });
        it('should go to last page', async () => {
          const lastPageButton = await getPageButtonByLabel('8');
          await lastPageButton.click();

          expectGetApplicationLogs(
            fakeLogsResponse({
              data: [
                fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
                fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
              ],
              metadata: {
                'my-api': { name: 'My API', version: '1.0' },
                'my-api-2': { name: 'My API 2', version: '2.0' },
                'my-plan': { name: 'My Plan' },
                data: { total: 79 },
              },
            }),
            8,
          );
          fixture.detectChanges();
        });
      });
    });
  });

  describe('Page 3 in query params', () => {
    beforeEach(async () => {
      await init({ page: 3 });
    });

    describe('Third page of many pages of results', () => {
      beforeEach(async () => {
        expectGetApplicationLogs(
          fakeLogsResponse({
            data: [
              fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
              fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
            ],
            metadata: {
              'my-api': { name: 'My API', version: '1.0' },
              'my-api-2': { name: 'My API 2', version: '2.0' },
              'my-plan': { name: 'My Plan' },
              data: { total: 79 },
            },
          }),
          3,
        );
        expectGetSubscriptions(fakeSubscriptionResponse());
      });
      it('should allow previous page', async () => {
        const previousPageButton = await getPreviousPageButton();
        expect(previousPageButton).toBeTruthy();
        expect(await previousPageButton.isDisabled()).toEqual(false);
      });
      it('should allow next page', async () => {
        const nextPageButton = await getNextPageButton();
        expect(nextPageButton).toBeTruthy();
        expect(await nextPageButton.isDisabled()).toEqual(false);
      });
      it('should show "1" for first page', async () => {
        const firstPageButton = await getPageButtonByLabel('1');
        expect(firstPageButton).toBeTruthy();
        expect(await firstPageButton.isDisabled()).toEqual(false);
      });
      it('should show "2" for previous page', async () => {
        const secondPageButton = await getPageButtonByLabel('2');
        expect(secondPageButton).toBeTruthy();
        expect(await secondPageButton.isDisabled()).toEqual(false);
      });
      it('should show "8" for last page', async () => {
        const lastPageButton = await getPageButtonByLabel('8');
        expect(lastPageButton).toBeTruthy();
        expect(await lastPageButton.isDisabled()).toEqual(false);
      });
      it('should highlight current page', async () => {
        const currentPaginationPage = await getCurrentPaginationPage();
        expect(currentPaginationPage).toBeTruthy();
        expect(await currentPaginationPage.getText()).toEqual('3');
      });
      it('should go to previous page via arrow', async () => {
        const previousPageButton = await getPreviousPageButton();
        await previousPageButton.click();

        expectGetApplicationLogs(
          fakeLogsResponse({
            data: [
              fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
              fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
            ],
            metadata: {
              'my-api': { name: 'My API', version: '1.0' },
              'my-api-2': { name: 'My API 2', version: '2.0' },
              'my-plan': { name: 'My Plan' },
              data: { total: 79 },
            },
          }),
          2,
        );
        fixture.detectChanges();
      });
    });
  });

  describe('Filters', () => {
    describe('No filters in query params', () => {
      beforeEach(async () => {
        await init({});

        expectGetApplicationLogs(
          fakeLogsResponse({
            data: [
              fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
              fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
            ],
            metadata: {
              'my-api': { name: 'My API', version: '1.0' },
              'my-api-2': { name: 'My API 2', version: '2.0' },
              'my-plan': { name: 'My Plan' },
              data: { total: 3 },
            },
          }),
        );
        expectGetSubscriptions(fakeSubscriptionResponse());
        fixture.detectChanges();
      });

      it('should have no filters selected', async () => {
        expect(await noChipFiltersDisplayed()).toEqual(true);
      });
      it('should have search and reset buttons disabled', async () => {
        const resetButton = await getResetFilterButton();
        expect(await resetButton.isDisabled()).toEqual(true);

        const searchButton = await getSearchButton();
        expect(await searchButton.isDisabled()).toEqual(true);
      });
    });
    describe('APIs', () => {
      describe('One API in query params', () => {
        const API_ID = 'api-id';
        beforeEach(async () => {
          await init({ apis: [API_ID] });

          expectGetApplicationLogs(
            fakeLogsResponse({
              data: [
                fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
                fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
              ],
              metadata: {
                'my-api': { name: 'My API', version: '1.0' },
                'my-api-2': { name: 'My API 2', version: '2.0' },
                'my-plan': { name: 'My Plan' },
                data: { total: 3 },
              },
            }),
            1,
            `(api:\\"${API_ID}\\")`,
          );
          expectGetSubscriptions(
            fakeSubscriptionResponse({
              data: [fakeSubscription({ api: API_ID })],
              metadata: { [API_ID]: { name: 'API 1', apiVersion: '99' } },
            }),
          );
          fixture.detectChanges();
        });

        it('should have api pre-selected', async () => {
          expect(await noChipFiltersDisplayed()).toEqual(false);
          const apiFilter = await getApiSelection();
          expect(await apiFilter.isEmpty()).toEqual(false);
          expect(await apiFilter.getValueText()).toEqual('API 1 (99)');
        });

        it('should have search button disabled on load', async () => {
          const searchButton = await getSearchButton();
          expect(await searchButton.isDisabled()).toEqual(true);
        });

        it('should reset filter', async () => {
          const resetButton = await getResetFilterButton();
          expect(await resetButton.isDisabled()).toEqual(false);
          await resetButton.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);
        });
      });
      describe('Two APIs in query params', () => {
        const API_ID_1 = 'api-id-1';
        const API_ID_2 = 'api-id-2';
        const API_ID_3 = 'api-id-3';
        beforeEach(async () => {
          await init({ apis: [API_ID_1, API_ID_2] });

          expectGetApplicationLogs(
            fakeLogsResponse({
              data: [
                fakeLog({ api: 'my-api', plan: 'my-plan', status: 201, timestamp: 1466424490000 }),
                fakeLog({ api: 'my-api-2', plan: 'my-plan', status: 204, timestamp: 1566424490000 }),
              ],
              metadata: {
                'my-api': { name: 'My API', version: '1.0' },
                'my-api-2': { name: 'My API 2', version: '2.0' },
                'my-plan': { name: 'My Plan' },
                data: { total: 3 },
              },
            }),
            1,
            `(api:\\"${API_ID_1}\\" OR \\"${API_ID_2}\\")`,
          );
          expectGetSubscriptions(
            fakeSubscriptionResponse({
              data: [fakeSubscription({ api: API_ID_1 }), fakeSubscription({ api: API_ID_2 }), fakeSubscription({ api: API_ID_3 })],
              metadata: {
                [API_ID_1]: { name: 'API 1', apiVersion: '99' },
                [API_ID_2]: { name: 'API 2', apiVersion: '1' },
                [API_ID_3]: { name: 'API 3', apiVersion: '24' },
              },
            }),
          );
          fixture.detectChanges();
        });

        it('should have two apis pre-selected', async () => {
          expect(await harnessLoader.getAllHarnesses(MatChipHarness)).toHaveLength(2);

          const api1Chip = await getChipFilter('API 1 (99)');
          expect(api1Chip).toBeTruthy();

          const api2Chip = await getChipFilter('API 2 (1)');
          expect(api2Chip).toBeTruthy();

          const apiFilter = await getApiSelection();
          expect(await apiFilter.isEmpty()).toEqual(false);
          expect(await apiFilter.getValueText()).toEqual('API 1 (99), API 2 (1)');
        });
      });
    });

    describe('HTTP Methods', () => {
      describe('One method in query params', () => {
        const GET_METHOD = '3';
        beforeEach(async () => {
          await init({ methods: [GET_METHOD] });

          expectGetApplicationLogs(fakeLogsResponse(), 1, `(method:\\"${GET_METHOD}\\")`);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });

        it('should have method pre-selected', async () => {
          expect(await noChipFiltersDisplayed()).toEqual(false);
          const httpMethodFilter = await getHttpMethodSelection();
          expect(await httpMethodFilter.isEmpty()).toEqual(false);
          expect(await httpMethodFilter.getValueText()).toEqual('GET');

          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(1);

          expect(await filterChips[0].getText()).toEqual('GET');
        });

        it('should select POST HTTP Method filter', async () => {
          const httpMethodFilter = await getHttpMethodSelection();
          await httpMethodFilter.open();
          await httpMethodFilter.clickOptions({ text: 'POST' });
          await httpMethodFilter.close();
          expect(await httpMethodFilter.getValueText()).toEqual('GET, POST');

          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(2);

          expect(await filterChips[1].getText()).toEqual('POST');

          const searchButton = await getSearchButton();
          expect(await searchButton.isDisabled()).toEqual(false);

          await searchButton.click();
          expectGetApplicationLogs(fakeLogsResponse(), 1, `(method:\\"${GET_METHOD}\\" OR \\"7\\")`);
        });
        it('should reset filter', async () => {
          const resetButton = await getResetFilterButton();
          expect(await resetButton.isDisabled()).toEqual(false);
          await resetButton.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);
        });
      });
    });

    describe('Response Times', () => {
      describe('One method in query params', () => {
        const RESPONSE_TIME = '0 TO 100';
        beforeEach(async () => {
          await init({ responseTimes: RESPONSE_TIME });

          expectGetApplicationLogs(fakeLogsResponse(), 1, `(response-time:[${RESPONSE_TIME}])`);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });

        it('should have method pre-selected', async () => {
          expect(await noChipFiltersDisplayed()).toEqual(false);
          const responseTimeSelection = await getResponseTimesSelection();
          expect(await responseTimeSelection.isEmpty()).toEqual(false);
          expect(await responseTimeSelection.getValueText()).toEqual('< 100 ms');

          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(1);

          expect(await filterChips[0].getText()).toEqual('Response time < 100 ms');
        });

        it('should select 100 - 200 response time filter', async () => {
          const responseTimeSelection = await getResponseTimesSelection();
          await responseTimeSelection.open();
          await responseTimeSelection.clickOptions({ text: '100 to 200 ms' });
          await responseTimeSelection.close();
          expect(await responseTimeSelection.getValueText()).toEqual('< 100 ms, 100 to 200 ms');

          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(2);

          expect(await filterChips[1].getText()).toEqual('Response time 100 - 200 ms');

          const searchButton = await getSearchButton();
          expect(await searchButton.isDisabled()).toEqual(false);

          await searchButton.click();
          expectGetApplicationLogs(fakeLogsResponse(), 1, `(response-time:[${RESPONSE_TIME}] OR [100 TO 200])`);
        });
        it('should reset filter', async () => {
          const resetButton = await getResetFilterButton();
          expect(await resetButton.isDisabled()).toEqual(false);
          await resetButton.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);
        });
      });
    });

    describe('Period', () => {
      describe('No period in query params', () => {
        beforeEach(async () => {
          await init({});

          expectGetApplicationLogs(fakeLogsResponse());
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });

        it('should have "Last day" pre-selected + empty filters', async () => {
          expect(await noChipFiltersDisplayed()).toEqual(true);

          const periodSelection = await getPeriodSelection();
          expect(await periodSelection.getValueText()).toEqual('Last day');
        });
      });
      describe('One period in query params', () => {
        const LAST_3_DAYS = '3d';
        beforeEach(async () => {
          await init({ period: LAST_3_DAYS });

          const dateMinusThreeDays = MOCK_DATE.getTime() - 86400000 * 3;

          expectGetApplicationLogs(fakeLogsResponse(), 1, undefined, undefined, dateMinusThreeDays);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });

        it('should have period pre-selected', async () => {
          const periodSelection = await getPeriodSelection();
          expect(await periodSelection.isEmpty()).toEqual(false);
          expect(await periodSelection.getValueText()).toEqual('Last 3 days');

          expect(await noChipFiltersDisplayed()).toEqual(true);
        });

        it('should select "Last 6 hours"', async () => {
          const periodSelection = await getPeriodSelection();
          await periodSelection.open();
          expect(await periodSelection.isOpen()).toEqual(true);
          await periodSelection.clickOptions({ text: 'Last day' });

          expect(await periodSelection.getValueText()).toEqual('Last day');

          expect(await noChipFiltersDisplayed()).toEqual(true);

          const searchButton = await getSearchButton();
          expect(await searchButton.isDisabled()).toEqual(false);

          await searchButton.click();
          expectGetApplicationLogs(fakeLogsResponse());
        });
        it('should reset filter but keep same period', async () => {
          const httpMethodsSelection = await getHttpMethodSelection();
          await httpMethodsSelection.open();
          await httpMethodsSelection.clickOptions({ text: 'GET' });

          const resetButton = await getResetFilterButton();
          expect(await resetButton.isDisabled()).toEqual(false);

          await resetButton.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);
          const periodSelection = await getPeriodSelection();
          expect(await periodSelection.isEmpty()).toEqual(false);
          expect(await periodSelection.getValueText()).toEqual('Last 3 days');
        });
      });
    });

    describe('Start + End Dates', () => {
      describe('Only start date in query params', () => {
        beforeEach(async () => {
          const from = MOCK_DATE.getTime() - 86400000 * 2; // From mock date - 2 days
          await init({ from });

          expectGetApplicationLogs(fakeLogsResponse(), 1, undefined, undefined, from);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });
        it('should be applied and shown in the filter chips', async () => {
          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(1);

          expect(await filterChips[0].getText()).toEqual('From: 2016-06-18');

          await openMoreFiltersDialog();
          fixture.detectChanges();

          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const startDatePicker = await dialog.getStartDatePicker();
          expect(await startDatePicker.getValue()).toEqual('6/18/2016');

          const endDatePicker = await dialog.getEndDatePicker();
          expect(await endDatePicker.getValue()).toEqual('');
        });
        it('should have the period drop-down disabled', async () => {
          const periodFilter = await getPeriodSelection();
          expect(await periodFilter.isDisabled()).toEqual(true);
        });
        it('should be able to add an end date', async () => {
          await openMoreFiltersDialog();
          fixture.detectChanges();

          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);

          const endDatePicker = await dialog.getEndDatePicker();
          await endDatePicker.openCalendar();
          const endDateCalendar = await endDatePicker.getCalendar();
          await endDateCalendar.selectCell({ text: '19' });
          expect(await endDatePicker.getValue()).toEqual('6/19/2016');

          await dialog.applyFilters();

          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(2);

          expect(await filterChips[0].getText()).toEqual('From: 2016-06-18');
          expect(await filterChips[1].getText()).toEqual('To: 2016-06-19');
        });
        it('should reset', async () => {
          const resetBtn = await getResetFilterButton();
          await resetBtn.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);

          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);

          const startDatePicker = await dialog.getStartDatePicker();
          expect(await startDatePicker.getValue()).toEqual('');

          const endDatePicker = await dialog.getEndDatePicker();
          expect(await endDatePicker.getValue()).toEqual('');
        });
      });

      describe('Only Start date + Period in query params', () => {
        beforeEach(async () => {
          const from = MOCK_DATE.getTime() - 86400000 * 2; // From mock date - 2 days
          const period = '14d';
          await init({ from, period });

          expectGetApplicationLogs(fakeLogsResponse(), 1, undefined, undefined, from);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });
        it('should only apply start date', async () => {
          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(await filterChips[0].getText()).toEqual('From: 2016-06-18');

          await openMoreFiltersDialog();
          fixture.detectChanges();

          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const startDatePicker = await dialog.getStartDatePicker();
          expect(await startDatePicker.getValue()).toEqual('6/18/2016');
        });
        it('should have the period drop-down disabled', async () => {
          const periodFilter = await getPeriodSelection();
          expect(await periodFilter.isDisabled()).toEqual(true);
          expect(await periodFilter.getValueText()).toEqual('Last 14 days');
        });
      });

      describe('Only End date in query params', () => {
        beforeEach(async () => {
          const to = MOCK_DATE.getTime() - 86400000 * 2; // To mock date - 2 days
          await init({ to });

          expectGetApplicationLogs(fakeLogsResponse(), 1, undefined, to);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });
        it('should only apply end date', async () => {
          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(1);

          expect(await filterChips[0].getText()).toEqual('To: 2016-06-18');

          await openMoreFiltersDialog();
          fixture.detectChanges();

          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const endDatePicker = await dialog.getEndDatePicker();
          expect(await endDatePicker.getValue()).toEqual('6/18/2016');
        });
        it('should apply "Last day" by default', async () => {
          const periodFilter = await getPeriodSelection();
          expect(await periodFilter.isDisabled()).toEqual(false);
          expect(await periodFilter.getValueText()).toEqual('Last day');
        });
        it('should be able to add start date', async () => {
          await openMoreFiltersDialog();
          fixture.detectChanges();

          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const startDatePicker = await dialog.getStartDatePicker();
          await startDatePicker.setValue('6/10/2016');
          await dialog.applyFilters();

          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(2);

          expect(await filterChips[0].getText()).toEqual('From: 2016-06-10');
          expect(await filterChips[1].getText()).toEqual('To: 2016-06-18');
        });
        it('should reset', async () => {
          const resetBtn = await getResetFilterButton();
          await resetBtn.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);

          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);

          const startDatePicker = await dialog.getStartDatePicker();
          expect(await startDatePicker.getValue()).toEqual('');

          const endDatePicker = await dialog.getEndDatePicker();
          expect(await endDatePicker.getValue()).toEqual('');
        });
      });

      describe('Only End date + Period in query params', () => {
        const to = MOCK_DATE.getTime() - 86400000 * 2; // From mock date - 2 days
        beforeEach(async () => {
          const from = to - 86400000 * 14; // 14 days before the 'to' date
          const period = '14d';
          await init({ to, period });

          expectGetApplicationLogs(fakeLogsResponse(), 1, undefined, to, from);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });
        it('should apply end date + period', async () => {
          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(1);

          expect(await filterChips[0].getText()).toEqual('To: 2016-06-18');

          const periodSelection = await getPeriodSelection();
          expect(await periodSelection.getValueText()).toEqual('Last 14 days');
        });
        it('should have period drop-down enabled', async () => {
          const periodSelection = await getPeriodSelection();
          expect(await periodSelection.isDisabled()).toEqual(false);
        });
        it('should be able to add start date + disabled period drop-down', async () => {
          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const startDatePicker = await dialog.getStartDatePicker();
          await startDatePicker.setValue('6/10/2016');
          await dialog.applyFilters();

          const periodSelection = await getPeriodSelection();
          expect(await periodSelection.isDisabled()).toEqual(true);

          await getSearchButton().then(btn => btn.click());

          expectGetApplicationLogs(fakeLogsResponse(), 1, undefined, to, new Date('6/10/2016').getTime());
        });
      });

      describe('Only end date + start date in query params', () => {
        beforeEach(async () => {
          const to = MOCK_DATE.getTime() - 86400000 * 2; // From mock date - 2 days
          const from = to - 86400000 * 14; // 14 days before the 'to' date
          await init({ to, from });

          expectGetApplicationLogs(fakeLogsResponse(), 1, undefined, to, from);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });
        it('should apply start date + end date', async () => {
          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(2);

          expect(await filterChips[0].getText()).toEqual('From: 2016-06-04');
          expect(await filterChips[1].getText()).toEqual('To: 2016-06-18');
        });
        it('should have the period drop-down disabled + "Last day"', async () => {
          const periodFilter = await getPeriodSelection();
          expect(await periodFilter.isDisabled()).toEqual(true);
          expect(await periodFilter.getValueText()).toEqual('Last day');
        });
      });

      describe('End date + start date + period in query params', () => {
        beforeEach(async () => {
          const to = MOCK_DATE.getTime() - 86400000 * 2; // From mock date - 2 days
          const from = to - 86400000 * 14; // 14 days before the 'to' date
          const period = '14d';
          await init({ to, from, period });

          expectGetApplicationLogs(fakeLogsResponse(), 1, undefined, to, from);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });
        it('should apply start date + end date', async () => {
          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(2);

          expect(await filterChips[0].getText()).toEqual('From: 2016-06-04');
          expect(await filterChips[1].getText()).toEqual('To: 2016-06-18');
        });
        it('should have the period drop-down disabled set to query param period', async () => {
          const periodFilter = await getPeriodSelection();
          expect(await periodFilter.isDisabled()).toEqual(true);
          expect(await periodFilter.getValueText()).toEqual('Last 14 days');
        });
      });
    });

    describe('Request ID', () => {
      describe('One request ID in query params', () => {
        const REQUEST_ID = 'my-request';
        beforeEach(async () => {
          await init({ requestId: REQUEST_ID });

          expectGetApplicationLogs(fakeLogsResponse(), 1, `(_id:\\"${REQUEST_ID}\\")`);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });

        it('should have request id pre-selected', async () => {
          expect(await noChipFiltersDisplayed()).toEqual(false);
          const chips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(chips).toHaveLength(1);
          expect(await chips[0].getText()).toEqual(`Request ID: ${REQUEST_ID}`);

          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const requestIdInput = await dialog.getRequestIdInput();
          expect(await requestIdInput.getValue()).toEqual(REQUEST_ID);
        });

        it('should change Request ID filter', async () => {
          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const requestIdInput = await dialog.getRequestIdInput();
          await requestIdInput.setValue('new-request-id');
          await dialog.applyFilters();

          const searchButton = await getSearchButton();
          expect(await searchButton.isDisabled()).toEqual(false);

          await searchButton.click();
          expectGetApplicationLogs(fakeLogsResponse(), 1, `(_id:\\"new-request-id\\")`);
        });
        it('should reset filter', async () => {
          const resetButton = await getResetFilterButton();
          expect(await resetButton.isDisabled()).toEqual(false);
          await resetButton.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);
        });
      });
    });

    describe('Transaction ID', () => {
      describe('One transaction ID in query params', () => {
        const TRANSACTION_ID = 'my-transaction';
        beforeEach(async () => {
          await init({ transactionId: TRANSACTION_ID });

          expectGetApplicationLogs(fakeLogsResponse(), 1, `(transaction:\\"${TRANSACTION_ID}\\")`);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });

        it('should have transaction id pre-selected', async () => {
          expect(await noChipFiltersDisplayed()).toEqual(false);
          const chips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(chips).toHaveLength(1);
          expect(await chips[0].getText()).toEqual(`Transaction ID: ${TRANSACTION_ID}`);

          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const transactionIdInput = await dialog.getTransactionIdInput();
          expect(await transactionIdInput.getValue()).toEqual(TRANSACTION_ID);
        });

        it('should change Transaction ID filter', async () => {
          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const transactionIdInput = await dialog.getTransactionIdInput();
          await transactionIdInput.setValue('new-transaction-id');
          await dialog.applyFilters();

          const searchButton = await getSearchButton();
          expect(await searchButton.isDisabled()).toEqual(false);

          await searchButton.click();
          expectGetApplicationLogs(fakeLogsResponse(), 1, `(transaction:\\"new-transaction-id\\")`);
        });
        it('should reset filter', async () => {
          const resetButton = await getResetFilterButton();
          expect(await resetButton.isDisabled()).toEqual(false);
          await resetButton.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);
        });
      });
    });

    describe('HTTP Statuses', () => {
      describe('One method in query params', () => {
        const OK = '200';
        beforeEach(async () => {
          await init({ httpStatuses: OK });

          expectGetApplicationLogs(fakeLogsResponse(), 1, `(status:\\"${OK}\\")`);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });

        it('should have HTTP Status pre-selected', async () => {
          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(1);
          expect(await filterChips[0].getText()).toEqual('HTTP Status: 200 - OK');

          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const httpStatusSelection = await dialog.getHttpStatusSelection();
          expect(await httpStatusSelection.getValueText()).toEqual('200 - OK');
        });

        it('should select 404 HTTP Status filter', async () => {
          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const httpStatusSelection = await dialog.getHttpStatusSelection();
          await httpStatusSelection.open();
          await httpStatusSelection.clickOptions({ text: '404 - Not Found' });
          expect(await httpStatusSelection.getValueText()).toEqual('200 - OK, 404 - Not Found');

          await dialog.applyFilters();

          const filterChips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(filterChips).toHaveLength(2);

          expect(await filterChips[1].getText()).toEqual('HTTP Status: 404 - Not Found');

          const searchButton = await getSearchButton();
          expect(await searchButton.isDisabled()).toEqual(false);

          await searchButton.click();
          expectGetApplicationLogs(fakeLogsResponse(), 1, `(status:\\"${OK}\\" OR \\"404\\")`);
        });
        it('should reset filter', async () => {
          const resetButton = await getResetFilterButton();
          expect(await resetButton.isDisabled()).toEqual(false);
          await resetButton.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);
        });
      });
    });

    describe('Message text', () => {
      describe('Message text defined in query params', () => {
        const MESSAGE_TEXT = 'find me';
        beforeEach(async () => {
          await init({ messageText: MESSAGE_TEXT });

          expectGetApplicationLogs(fakeLogsResponse(), 1, `(body:*${MESSAGE_TEXT}*)`);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });

        it('should have message text pre-filled', async () => {
          expect(await noChipFiltersDisplayed()).toEqual(false);
          const chips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(chips).toHaveLength(1);
          expect(await chips[0].getText()).toEqual(`Message body includes: ${MESSAGE_TEXT}`);

          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const messageTextInput = await dialog.getMessageTextInput();
          expect(await messageTextInput.getValue()).toEqual(MESSAGE_TEXT);
        });

        it('should change message text filter', async () => {
          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const messageTextInput = await dialog.getMessageTextInput();
          await messageTextInput.setValue('actually find this');
          await dialog.applyFilters();

          const searchButton = await getSearchButton();
          expect(await searchButton.isDisabled()).toEqual(false);

          await searchButton.click();
          expectGetApplicationLogs(fakeLogsResponse(), 1, `(body:*actually find this*)`);
        });
        it('should reset filter', async () => {
          const resetButton = await getResetFilterButton();
          expect(await resetButton.isDisabled()).toEqual(false);
          await resetButton.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);
        });
      });
    });

    describe('Path', () => {
      describe('Path defined in query params', () => {
        const PATH = 'a-nice-path';
        beforeEach(async () => {
          await init({ path: PATH });

          expectGetApplicationLogs(fakeLogsResponse(), 1, `(uri:*${PATH}*)`);
          expectGetSubscriptions(fakeSubscriptionResponse());
          fixture.detectChanges();
        });

        it('should have path pre-filled', async () => {
          expect(await noChipFiltersDisplayed()).toEqual(false);
          const chips = await harnessLoader.getAllHarnesses(MatChipHarness);
          expect(chips).toHaveLength(1);
          expect(await chips[0].getText()).toEqual(`Path: ${PATH}`);

          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const pathInput = await dialog.getPathInput();
          expect(await pathInput.getValue()).toEqual(PATH);
        });

        it('should change path filter', async () => {
          await openMoreFiltersDialog();
          const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
          const pathInput = await dialog.getPathInput();
          await pathInput.setValue('/different-path');
          await dialog.applyFilters();

          const searchButton = await getSearchButton();
          expect(await searchButton.isDisabled()).toEqual(false);

          await searchButton.click();
          expectGetApplicationLogs(fakeLogsResponse(), 1, `(uri:*\\\\/different-path*)`);
        });
        it('should reset filter', async () => {
          const resetButton = await getResetFilterButton();
          expect(await resetButton.isDisabled()).toEqual(false);
          await resetButton.click();

          expect(await noChipFiltersDisplayed()).toEqual(true);
        });
      });
    });
  });

  function expectGetApplicationLogs(logsResponse: LogsResponse, page: number = 1, query?: string, to?: number, from?: number) {
    const toInMilliseconds = to ?? Date.now();
    const fromInMilliseconds = from ?? toInMilliseconds - 86400000;
    httpTestingController
      .expectOne(
        `${TESTING_BASE_URL}/applications/${APP_ID}/logs?page=${page}&size=10&from=${fromInMilliseconds}&to=${toInMilliseconds}&order=DESC&field=@timestamp` +
          `${query ? '&query=' + query : ''}`,
      )
      .flush(logsResponse);
  }

  function expectGetSubscriptions(subscriptionsResponse: SubscriptionsResponse) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/subscriptions?applicationId=${APP_ID}&statuses=ACCEPTED&statuses=PAUSED&size=-1`)
      .flush(subscriptionsResponse);
  }

  function getNoLogsMessageSection(): DebugElement {
    return fixture.debugElement.query(By.css('.no-logs'));
  }

  async function getTextByRowIndexAndColumnName(i: number, columnName: string): Promise<string> {
    return await harnessLoader
      .getHarness(MatTableHarness)
      .then(table => table.getRows())
      .then(rows => rows[i].getCells({ columnName }))
      .then(cells => cells[0].getText());
  }

  async function getPreviousPageButton(): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Previous page of logs"]', variant: 'icon' }));
  }

  async function getNextPageButton(): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Next page of logs"]', variant: 'icon' }));
  }

  async function getPageButtonByLabel(label: string): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ text: label }));
  }

  async function getCurrentPaginationPage(): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Current page of logs"]' }));
  }

  async function noChipFiltersDisplayed(): Promise<boolean> {
    return await harnessLoader.getAllHarnesses(MatChipHarness).then(harnesses => harnesses.length === 0);
  }

  async function getChipFilter(text: string): Promise<MatChipHarness> {
    return await harnessLoader.getHarness(MatChipHarness.with({ text }));
  }

  async function getApiSelection(): Promise<MatSelectHarness> {
    return await harnessLoader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Filter by API"]' }));
  }

  async function getHttpMethodSelection(): Promise<MatSelectHarness> {
    return await harnessLoader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Filter by HTTP Method"]' }));
  }

  async function getResponseTimesSelection(): Promise<MatSelectHarness> {
    return await harnessLoader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Filter by Response Time"]' }));
  }

  async function getPeriodSelection(): Promise<MatSelectHarness> {
    return await harnessLoader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Filter by Period"]' }));
  }

  async function getResetFilterButton(): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Reset filters"]' }));
  }

  async function getSearchButton(): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Apply filters"]' }));
  }

  async function openMoreFiltersDialog(): Promise<void> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add more filters"]' })).then(btn => btn.click());
  }
});
