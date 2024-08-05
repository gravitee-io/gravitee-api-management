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
import { MatTableHarness } from '@angular/material/table/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { ApplicationTabLogsComponent } from './application-tab-logs.component';
import { fakeApplication } from '../../../../entities/application/application.fixture';
import { LogsResponse } from '../../../../entities/log/log';
import { fakeLog, fakeLogsResponse } from '../../../../entities/log/log.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ApplicationTabLogsComponent', () => {
  let component: ApplicationTabLogsComponent;
  let fixture: ComponentFixture<ApplicationTabLogsComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  const APP_ID = 'app-id';
  const MOCK_DATE = new Date(1466424490000);

  const init = async (queryParams: unknown) => {
    const asBehaviorSubject = new BehaviorSubject(queryParams);

    await TestBed.configureTestingModule({
      imports: [ApplicationTabLogsComponent, AppTestingModule],
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
    component = fixture.componentInstance;
    component.application = fakeApplication({ id: APP_ID });

    const router = TestBed.inject(Router);
    jest.spyOn(router, 'navigate').mockImplementation((_, configuration) => {
      asBehaviorSubject.next(configuration?.queryParams ?? {});
      return new Promise(_ => true);
    });

    jest.useFakeTimers().setSystemTime(MOCK_DATE);
    fixture.detectChanges();
  };
  afterEach(() => {
    httpTestingController.verify();
  });

  describe('No page in query params', () => {
    beforeEach(async () => {
      await init({});
    });

    it('should show empty message when no logs', async () => {
      expectGetApplicationLogs(fakeLogsResponse({ data: [], metadata: { data: { total: 0 } } }));
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

  function expectGetApplicationLogs(logsResponse: LogsResponse, page: number = 1) {
    const currentDateInMilliseconds = MOCK_DATE.getTime();
    const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
    httpTestingController
      .expectOne(
        `${TESTING_BASE_URL}/applications/${APP_ID}/logs?page=${page}&size=10&from=${yesterdayInMilliseconds}&to=${currentDateInMilliseconds}&order=DESC&field=@timestamp`,
      )
      .flush(logsResponse);
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
});
