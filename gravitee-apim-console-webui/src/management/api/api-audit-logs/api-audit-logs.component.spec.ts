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
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';

import { ApiAuditLogsComponent } from './api-audit-logs.component';
import { ApiAuditLogsModule } from './api-audit-logs.module';
import { ApiAuditsFilterFormHarness, ApiAuditsTableHarness, ApiEventsTableHarness } from './components';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import {
  fakeAuditResponse,
  fakeEvent,
  fakeEventsResponse,
  Pagination,
  SearchApiAuditParam,
  SearchApiEventParam,
} from '../../../entities/management-api-v2';

describe('AuditLogsComponent', () => {
  const API_ID = 'an-api-id';

  let fixture: ComponentFixture<ApiAuditLogsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, GioTestingModule, ApiAuditLogsModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
    });

    fixture = TestBed.createComponent(ApiAuditLogsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    expectAuditEventsNameRequest();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Audits', () => {
    beforeEach(() => {
      expectApiEventsListRequest();
    });

    it('should display audit logs', async () => {
      expectAuditListRequest();

      const table = await loader.getHarness(ApiAuditsTableHarness);
      expect(await table.rows()).toEqual([
        {
          event: 'APIKEY_REVOKED',
          user: 'John Doe',
          date: 'Jul 1, 2021, 12:00:00 AM',
          patch: '',
          targets: 'API_KEY:d2df9def-fd47-491b-90be-ebf1829adb5b',
        },
      ]);
    });

    it('should refresh audits logs when filtering on events', async () => {
      expectAuditListRequest();

      const form = await loader.getHarness(ApiAuditsFilterFormHarness);
      await form.searchEvent('API_CREATED').then((options) => options[0]?.click());

      expectAuditListRequest({ events: 'API_CREATED' });
    });

    it('should refresh audits logs when filtering on date range', async () => {
      expectAuditListRequest();

      const form = await loader.getHarness(ApiAuditsFilterFormHarness);
      await form.setDateRange({ start: '4/11/2022', end: '4/20/2022' });

      expectAuditListRequest({ from: 1649635200000 });
      expectAuditListRequest({ from: 1649635200000, to: 1650499199999 });
    });

    it('should reset filters and pagination', async () => {
      expectAuditListRequest(
        undefined,
        undefined,
        fakeAuditResponse({ pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
      );

      const table = await loader.getHarness(ApiAuditsTableHarness);
      const paginator = await table.paginator();
      const form = await loader.getHarness(ApiAuditsFilterFormHarness);

      // fill events
      await form.searchEvent('API_CREATED').then((options) => options[0]?.click());
      expectAuditListRequest(
        { events: 'API_CREATED' },
        { page: 1, perPage: 10 },
        fakeAuditResponse({ pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
      );

      // fill date range
      await form.setDateRange({ start: '4/11/2022', end: '4/20/2022' });
      expectAuditListRequest(
        { events: 'API_CREATED', from: 1649635200000 },
        { page: 1, perPage: 10 },
        fakeAuditResponse({ pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
      );
      expectAuditListRequest(
        { events: 'API_CREATED', from: 1649635200000, to: 1650499199999 },
        { page: 1, perPage: 10 },
        fakeAuditResponse({ pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
      );

      // navigate to 2nd page
      await paginator.goToNextPage();
      expectAuditListRequest(
        { events: 'API_CREATED', from: 1649635200000, to: 1650499199999 },
        { page: 2, perPage: 10 },
        fakeAuditResponse({ pagination: { page: 2, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
      );

      // reset
      await form.reset();
      expectAuditListRequest(
        { events: '', from: null, to: null },
        { page: 1, perPage: 10 },
        fakeAuditResponse({ pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
      );
    });

    describe('pagination', () => {
      it('should disable next & previous page links when only one page', async () => {
        expectAuditListRequest(
          undefined,
          undefined,
          fakeAuditResponse({ pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 5, totalCount: 5 } }),
        );
        const table = await loader.getHarness(ApiAuditsTableHarness);
        const paginator = await table.paginator();

        expect(await paginator.isNextPageDisabled()).toEqual(true);
        expect(await paginator.isPreviousPageDisabled()).toEqual(true);
      });

      it('should enable next & previous page links', async () => {
        expectAuditListRequest(
          undefined,
          { page: 1, perPage: 10 },
          fakeAuditResponse({ pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
        );
        const table = await loader.getHarness(ApiAuditsTableHarness);
        const paginator = await table.paginator();

        // 1st page
        expect(await paginator.isNextPageDisabled()).toEqual(false);
        expect(await paginator.isPreviousPageDisabled()).toEqual(true);

        // navigate to 2nd page
        await paginator.goToNextPage();
        expectAuditListRequest(
          undefined,
          { page: 2, perPage: 10 },
          fakeAuditResponse({ pagination: { page: 2, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
        );
        expect(await paginator.isNextPageDisabled()).toEqual(false);
        expect(await paginator.isPreviousPageDisabled()).toEqual(false);

        // navigate to 3rd and last page
        await paginator.goToNextPage();
        expectAuditListRequest(
          undefined,
          { page: 3, perPage: 10 },
          fakeAuditResponse({ pagination: { page: 3, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
        );
        expect(await paginator.isNextPageDisabled()).toEqual(true);
        expect(await paginator.isPreviousPageDisabled()).toEqual(false);
      });
    });
  });

  describe('Events', () => {
    beforeEach(() => {
      expectAuditListRequest();
      expectApiEventsListRequest(
        undefined,
        undefined,
        fakeEventsResponse({
          data: [fakeEvent({ type: 'PUBLISH_API' }), fakeEvent({ type: 'START_API' }), fakeEvent({ type: 'STOP_API' })],
        }),
      );
    });

    it('should display events', async () => {
      const table = await loader.getHarness(ApiEventsTableHarness);
      expect(await table.rows()).toEqual([
        {
          icon: '',
          user: 'John Doe',
          createdAt: 'Jan 1, 2021, 12:00:00 AM',
          type: 'Deployed',
        },
        {
          icon: '',
          user: 'John Doe',
          createdAt: 'Jan 1, 2021, 12:00:00 AM',
          type: 'Started',
        },
        {
          icon: '',
          user: 'John Doe',
          createdAt: 'Jan 1, 2021, 12:00:00 AM',
          type: 'Stopped',
        },
      ]);
    });
  });

  function expectAuditListRequest(
    filters: SearchApiAuditParam = {},
    pagination: Pagination = { page: 1, perPage: 10 },
    response = fakeAuditResponse(),
  ) {
    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/audits?page=${pagination.page}&perPage=${pagination.perPage}${
        filters.from ? '&from=' + filters.from : ''
      }${filters.to ? '&to=' + filters.to : ''}${filters.events ? '&events=' + filters.events : ''}`,
    );
    if (!req.cancelled) req.flush(response);
  }

  function expectApiEventsListRequest(
    filters: SearchApiEventParam = { types: 'START_API,STOP_API,PUBLISH_API' },
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

  function expectAuditEventsNameRequest() {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/audits/events`);
    req.flush({ data: ['APIKEY_REVOKED', 'API_CREATED', 'API_UPDATED'] });
  }
});
