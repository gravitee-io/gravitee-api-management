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

import { ApiRuntimeLogsModule } from './api-runtime-logs.module';
import { ApiRuntimeLogsComponent } from './api-runtime-logs.component';
import { ApiRuntimeLogsHarness } from './api-runtime-logs.component.harness';
import { ApiRuntimeLogsListRowHarness } from './components';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ApiV4, ConnectionLog, fakeApiV4 } from '../../../../entities/management-api-v2';
import { fakeApiLogsResponse, fakeEmptyApiLogsResponse } from '../../../../entities/management-api-v2/log/apiLogsResponse.fixture';
import { fakeConnectionLog } from '../../../../entities/management-api-v2/log/connectionLog.fixture';

describe('ApiRuntimeLogsComponent', () => {
  const API_ID = 'an-api-id';

  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiRuntimeLogsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiRuntimeLogsHarness;
  let logsRowHarness: ApiRuntimeLogsListRowHarness;

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
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, page: 1, perPage: 10 } },
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
      fixture.detectChanges();
      expectApiWithLogEnabled();
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
        fixture.detectChanges();
        expectApiWithLogEnabled();
      });

      it('should display the 1st page with default pagination', async () => {
        expect(await componentHarness.getRows()).toHaveLength(total);

        const paginator = await componentHarness.getPaginator();
        expect(await paginator.getPageSize()).toBe(10);
      });
    });

    describe('when there is more than one page', () => {
      const total = 50;
      const pageSize = 10;
      beforeEach(() => {
        expectApiWithLogs(total, pageSize);
        fixture.detectChanges();
        expectApiWithLogEnabled();
      });

      it('should display the 1st page', async () => {
        expect(await componentHarness.getRows()).toHaveLength(pageSize);

        const paginator = await componentHarness.getPaginator();
        expect(await paginator.getPageSize()).toBe(pageSize);
        expect(await paginator.getRangeLabel()).toBe(`1 – ${pageSize} of ${total}`);
      });

      it('should navigate to next page', async () => {
        const paginator = await componentHarness.getPaginator();
        await paginator.goToNextPage();

        expectSecondPage(total, pageSize);
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

        expectApiWithLogs(total, 25, 1);
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

  describe('GIVEN there are logs but logs are disabled', () => {
    beforeEach(async () => {
      await initComponent();
      expectApiWithLogs(1);
      fixture.detectChanges();
      expectApiWithLogDisabled();
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
      fixture.detectChanges();
      expectApiWithLogEnabled({ type: 'PROXY' });

      try {
        await logsRowHarness.getViewMessageButton();
      } catch (e) {
        expect(e.message).toMatch(/Failed to find element/);
      }
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

  function expectApiWithLogs(total: number, pageSize = 10, page = 1) {
    const itemsInPage = total < pageSize ? total : pageSize;

    const data: ConnectionLog[] = [];
    for (let i = 0; i < itemsInPage; i++) {
      data.push(fakeConnectionLog());
    }

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs?page=${page}&perPage=${pageSize}`,
        method: 'GET',
      })
      .flush(
        fakeApiLogsResponse({
          data,
          pagination: {
            totalCount: total,
            page: page,
            perPage: pageSize,
          },
        }),
      );
  }

  function expectSecondPage(total: number, pageSize = 10) {
    return expectApiWithLogs(total, pageSize, 2);
  }
});
