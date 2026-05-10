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
import { ComponentFixture, fakeAsync, flush, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiRuntimeLogsNativeComponent } from './api-runtime-logs-native.component';
import { ApiRuntimeLogsNativeHarness } from './api-runtime-logs-native.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import {
  fakeApiV4,
  fakeEmptyNativeApiLogsResponse,
  fakeNativeApiLogsResponse,
  fakePagedResult,
  fakePlanV4,
} from '../../../../entities/management-api-v2';
import { fakeApplication } from '../../../../entities/application/Application.fixture';

describe('ApiRuntimeLogsNativeComponent', () => {
  let fixture: ComponentFixture<ApiRuntimeLogsNativeComponent>;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;
  let harness: ApiRuntimeLogsNativeHarness;

  const API_ID = 'an-api-id';

  const initComponent = async (queryParams: Record<string, unknown> = {}) => {
    TestBed.configureTestingModule({
      imports: [ApiRuntimeLogsNativeComponent, MatIconTestingModule, GioTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID }, queryParams } } },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideNoopAnimations(),
      ],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsNativeComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    routerNavigateSpy = jest.spyOn(TestBed.inject(Router), 'navigate');
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsNativeHarness);
    fixture.detectChanges();
  };

  const expectApiCalls = () => {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`).flush(fakeApiV4({ id: API_ID, type: 'NATIVE' }));
    httpTestingController
      .match(req => req.url.includes(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans`))
      .forEach(req => req.flush({ data: [fakePlanV4({ id: 'plan-1', name: 'Plan 1' })], pagination: {} }));
    httpTestingController
      .match(req => req.url.includes(`${CONSTANTS_TESTING.env.baseURL}/applications/_paged`))
      .forEach(req => req.flush(fakePagedResult([fakeApplication({ id: 'app-1', name: 'App 1' })])));
  };

  const flushRowAppResolution = () => {
    httpTestingController
      .match(req => req.url.includes(`${CONSTANTS_TESTING.env.baseURL}/applications/_paged`) && req.params.has('ids'))
      .forEach(req =>
        req.flush(fakePagedResult([fakeApplication({ id: 'app-fc1bff13-6e25-4c4c-9bff-136e251c4cdf', name: 'Resolved App' })])),
      );
  };

  afterEach(() => {
    httpTestingController?.verify();
  });

  it('renders empty state when no rows', async () => {
    await initComponent();
    expectApiCalls();
    httpTestingController
      .expectOne(req => req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native` && req.method === 'GET')
      .flush(fakeEmptyNativeApiLogsResponse());
    fixture.detectChanges();

    expect(await harness.getEmptyStateText()).toContain('No data to display');
  });

  it('triggers initial search with apiId and pagination defaults', async () => {
    await initComponent();
    expectApiCalls();
    const req = httpTestingController.expectOne(
      r =>
        r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native` &&
        r.params.get('page') === '1' &&
        r.params.get('perPage') === '10',
    );
    expect(req.request.method).toBe('GET');
    req.flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
  });

  it('exposes Configure Reporting link', async () => {
    await initComponent();
    expectApiCalls();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native?page=1&perPage=10`)
      .flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    fixture.detectChanges();

    expect(await harness.getConfigureReportingLabel()).toBe('Configure Reporting');
  });

  it('shows reporting-disabled banner when reporterMetricsEnabled is false', async () => {
    await initComponent();
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`).flush(
      fakeApiV4({
        id: API_ID,
        type: 'NATIVE',
        analytics: { enabled: true, reporterMetricsEnabled: false },
      }),
    );
    httpTestingController
      .match(req => req.url.includes(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans`))
      .forEach(req => req.flush({ data: [], pagination: {} }));
    httpTestingController
      .match(req => req.url.includes(`${CONSTANTS_TESTING.env.baseURL}/applications/_paged`))
      .forEach(req => req.flush(fakePagedResult([])));
    httpTestingController
      .expectOne(req => req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native`)
      .flush(fakeEmptyNativeApiLogsResponse());
    fixture.detectChanges();

    expect(await harness.isReportingDisabledBannerVisible()).toBe(true);
  });

  it('refresh re-triggers search', fakeAsync(async () => {
    await initComponent();
    expectApiCalls();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native?page=1&perPage=10`)
      .flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();
    fixture.detectChanges();
    routerNavigateSpy.mockClear();

    fixture.componentInstance['refresh']();
    flush();

    httpTestingController
      .expectOne(req => req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native`)
      .flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();
    expect(routerNavigateSpy).toHaveBeenCalled();
  }));

  it('filter change triggers search with serialized NativeApiLogsParam shape', fakeAsync(async () => {
    await initComponent();
    expectApiCalls();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native?page=1&perPage=10`)
      .flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();
    fixture.detectChanges();

    fixture.componentInstance['form'].patchValue({
      applicationIds: ['app-1', 'app-2'],
      planIds: ['plan-1'],
      connectionStatuses: ['CONNECTED', 'CONNECTION_ERROR'],
    });
    flush();

    const req = httpTestingController.expectOne(
      r =>
        r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native` &&
        r.params.get('applicationIds') === 'app-1,app-2' &&
        r.params.get('planIds') === 'plan-1' &&
        r.params.get('connectionStatuses') === 'CONNECTED,CONNECTION_ERROR' &&
        r.params.get('page') === '1' &&
        r.params.get('perPage') === '10',
    );
    req.flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();
  }));

  it('pagination event triggers search with new page and perPage', fakeAsync(async () => {
    await initComponent();
    expectApiCalls();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native?page=1&perPage=10`)
      .flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();
    fixture.detectChanges();

    fixture.componentInstance['paginationUpdated']({ index: 3, size: 25 });
    flush();

    const req = httpTestingController.expectOne(
      r =>
        r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native` &&
        r.params.get('page') === '3' &&
        r.params.get('perPage') === '25',
    );
    req.flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();
  }));

  it('does not trigger a search when timeframe period switches to custom (waits for explicit Apply)', fakeAsync(async () => {
    await initComponent();
    expectApiCalls();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native?page=1&perPage=10`)
      .flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();
    fixture.detectChanges();

    fixture.componentInstance['form'].patchValue({ timeframe: { period: 'custom', from: null, to: null } });
    flush();

    httpTestingController.expectNone(req => req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native`);
  }));

  it('hydrates form from query params and fires initial search with from/to derived from preset', fakeAsync(async () => {
    await initComponent({ period: '1h' });
    expectApiCalls();

    const req = httpTestingController.expectOne(
      r =>
        r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native` &&
        !!r.params.get('from') &&
        !!r.params.get('to') &&
        r.params.get('page') === '1' &&
        r.params.get('perPage') === '10',
    );
    req.flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();

    // Verify URL sync also carried the period for shareable links
    expect(routerNavigateSpy).toHaveBeenCalledWith(
      ['.'],
      expect.objectContaining({ queryParams: expect.objectContaining({ period: '1h' }) }),
    );
  }));

  it('omits empty filter arrays from the query string', fakeAsync(async () => {
    await initComponent();
    expectApiCalls();

    const req = httpTestingController.expectOne(r => r.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native`);
    expect(req.request.params.has('applicationIds')).toBe(false);
    expect(req.request.params.has('planIds')).toBe(false);
    expect(req.request.params.has('connectionStatuses')).toBe(false);
    req.flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();
  }));

  it('keeps the search Subject alive after a failed search request', fakeAsync(async () => {
    await initComponent();
    expectApiCalls();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native?page=1&perPage=10`)
      .error(new ProgressEvent('Network error'), { status: 500, statusText: 'Internal Server Error' });
    flush();
    fixture.detectChanges();

    fixture.componentInstance['refresh']();
    flush();

    const retry = httpTestingController.expectOne(req => req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native`);
    retry.flush(fakeNativeApiLogsResponse());
    flushRowAppResolution();
    flush();
  }));

  it('still publishes logs when row name resolution fails', fakeAsync(async () => {
    await initComponent();
    expectApiCalls();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native?page=1&perPage=10`)
      .flush(fakeNativeApiLogsResponse());
    httpTestingController
      .match(req => req.url.includes(`${CONSTANTS_TESTING.env.baseURL}/applications/_paged`) && req.params.has('ids'))
      .forEach(req => req.error(new ProgressEvent('Network error'), { status: 500, statusText: 'Internal Server Error' }));
    flush();

    let publishedRows: number | null = null;
    fixture.componentInstance['logs$'].subscribe(r => (publishedRows = (r.data ?? []).length));
    flush();
    expect(publishedRows).toBeGreaterThan(0);
  }));
});
