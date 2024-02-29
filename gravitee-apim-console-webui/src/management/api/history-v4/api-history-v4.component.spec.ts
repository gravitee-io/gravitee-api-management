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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';
import { MatTableHarness } from '@angular/material/table/testing';

import { ApiHistoryV4Component } from './api-history-v4.component';
import { ApiHistoryV4DeploymentsTableComponent } from './deployments-table/api-history-v4-deployments-table.component';
import { ApiHistoryV4Module } from './api-history-v4.module';

import { fakeEvent, fakeEventsResponse, Pagination, SearchApiEventParam } from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

describe('ApiHistoryV4Component', () => {
  const API_ID = 'an-api-id';

  let fixture: ComponentFixture<ApiHistoryV4Component>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ApiHistoryV4Module, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
      declarations: [ApiHistoryV4Component, ApiHistoryV4DeploymentsTableComponent],
    });

    fixture = TestBed.createComponent(ApiHistoryV4Component);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Events', () => {
    it('should display events', async () => {
      expectApiEventsListRequest(
        undefined,
        undefined,
        fakeEventsResponse({
          data: [fakeEvent({ type: 'PUBLISH_API' })],
        }),
      );

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#deploymentsTable' }));
      expect(await table.getRows().then((value) => value[0].getCellTextByColumnName())).toEqual({
        version: '1',
        createdAt: 'Jan 1, 2021, 12:00:00 AM',
        user: 'John Doe',
        label: 'sample-label',
        action: '',
      });
    });

    it('should display an empty table', async () => {
      expectApiEventsListRequest(
        undefined,
        undefined,
        fakeEventsResponse({
          data: [],
        }),
      );

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#deploymentsTable' }));
      expect(await table.getCellTextByIndex().then((value) => value[0])).toEqual(['There is no published API (yet).']);
    });

    describe('pagination', () => {
      it('should disable next & previous page links when only one page', async () => {
        expectApiEventsListRequest(
          { types: 'PUBLISH_API' },
          undefined,
          fakeEventsResponse({ pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 5, totalCount: 5 } }),
        );
        const table = await loader.getHarness(GioTableWrapperHarness);
        const paginator = await table.getPaginator();

        expect(await paginator.isNextPageDisabled()).toEqual(true);
        expect(await paginator.isPreviousPageDisabled()).toEqual(true);
      });

      it('should enable next & previous page links', async () => {
        expectApiEventsListRequest(
          undefined,
          { page: 1, perPage: 10 },
          fakeEventsResponse({ pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
        );
        const table = await loader.getHarness(GioTableWrapperHarness);
        const paginator = await table.getPaginator();

        // 1st page
        expect(await paginator.isNextPageDisabled()).toEqual(false);
        expect(await paginator.isPreviousPageDisabled()).toEqual(true);

        // navigate to 2nd page
        await paginator.goToNextPage();
        expectApiEventsListRequest(
          { types: 'PUBLISH_API' },
          { page: 2, perPage: 10 },
          fakeEventsResponse({ pagination: { page: 2, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
        );
        expect(await paginator.isNextPageDisabled()).toEqual(false);
        expect(await paginator.isPreviousPageDisabled()).toEqual(false);

        // navigate to 3rd and last page
        await paginator.goToNextPage();
        expectApiEventsListRequest(
          undefined,
          { page: 3, perPage: 10 },
          fakeEventsResponse({ pagination: { page: 3, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
        );
        expect(await paginator.isNextPageDisabled()).toEqual(true);
        expect(await paginator.isPreviousPageDisabled()).toEqual(false);
      });
    });
  });

  function expectApiEventsListRequest(
    filters: SearchApiEventParam = { types: 'PUBLISH_API' },
    pagination: Pagination = { page: 1, perPage: 10 },
    response = fakeEventsResponse(),
  ) {
    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/events?page=${pagination.page}&perPage=${pagination.perPage}${
        filters.from ? '&from=' + filters.from : ''
      }${filters.to ? '&to=' + filters.to : ''}${filters.types ? '&types=' + filters.types : ''}`,
    );
    if (!req.cancelled) req.flush(response);
  }
});
