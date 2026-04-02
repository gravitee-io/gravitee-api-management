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
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { ApiAnalyticsStatusPieComponent } from './api-analytics-status-pie.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { timeFrameRangesParams } from '../../../../../../shared/utils/timeFrameRanges';
import { fakeAnalyticsGroupBy } from '../../../../../../entities/management-api-v2/analytics/analyticsUnified.fixture';

describe('ApiAnalyticsStatusPieComponent', () => {
  const API_ID = 'test-api-id';
  const analyticsUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;

  let fixture: ComponentFixture<ApiAnalyticsStatusPieComponent>;
  let httpTestingController: HttpTestingController;
  let apiAnalyticsV2Service: ApiAnalyticsV2Service;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsStatusPieComponent, NoopAnimationsModule, GioTestingModule],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    apiAnalyticsV2Service = TestBed.inject(ApiAnalyticsV2Service);

    fixture = TestBed.createComponent(ApiAnalyticsStatusPieComponent);
    fixture.componentInstance.apiId = API_ID;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display loading state while data is being fetched', () => {
    // Timing note: setTimeRangeFilter triggers the switchMap which sets isLoading=true
    // synchronously. The HTTP call remains pending (not flushed), so gio-loader is visible.
    // This relies on the loading flag being set *inside* the switchMap before the HTTP call.
    apiAnalyticsV2Service.setTimeRangeFilter(timeFrameRangesParams('1d'));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('gio-loader')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('gio-chart-pie')).toBeNull();

    // Flush pending request
    const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === analyticsUrl && r.params.get('type') === 'GROUP_BY');
    req.flush(fakeAnalyticsGroupBy());
  });

  it('should render pie chart when data is returned', () => {
    apiAnalyticsV2Service.setTimeRangeFilter(timeFrameRangesParams('1d'));
    fixture.detectChanges();

    const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === analyticsUrl && r.params.get('type') === 'GROUP_BY');
    req.flush(fakeAnalyticsGroupBy({ values: { '200': 50, '404': 10, '500': 5 } }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('gio-chart-pie')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('gio-loader')).toBeNull();
  });

  it('should display empty state when values object is empty', () => {
    apiAnalyticsV2Service.setTimeRangeFilter(timeFrameRangesParams('1d'));
    fixture.detectChanges();

    const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === analyticsUrl && r.params.get('type') === 'GROUP_BY');
    req.flush(fakeAnalyticsGroupBy({ values: {} }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('gio-card-empty-state')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('gio-chart-pie')).toBeNull();
  });

  it('should display error state when service throws', () => {
    apiAnalyticsV2Service.setTimeRangeFilter(timeFrameRangesParams('1d'));
    fixture.detectChanges();

    const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === analyticsUrl && r.params.get('type') === 'GROUP_BY');
    req.flush('Internal Server Error', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('gio-card-empty-state')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('gio-chart-pie')).toBeNull();
    expect(fixture.nativeElement.querySelector('gio-loader')).toBeNull();
  });

  it('should send request with status field to the unified analytics endpoint', () => {
    apiAnalyticsV2Service.setTimeRangeFilter(timeFrameRangesParams('1d'));
    fixture.detectChanges();

    const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === analyticsUrl && r.params.get('type') === 'GROUP_BY');
    expect(req.request.params.get('type')).toBe('GROUP_BY');
    expect(req.request.params.get('field')).toBe('status');
    req.flush(fakeAnalyticsGroupBy());
  });

  it('should re-fetch when time range filter emits a new value', () => {
    apiAnalyticsV2Service.setTimeRangeFilter(timeFrameRangesParams('1d'));
    fixture.detectChanges();

    // First call
    let req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === analyticsUrl && r.params.get('type') === 'GROUP_BY');
    req.flush(fakeAnalyticsGroupBy({ values: { '200': 10 } }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('gio-chart-pie')).toBeTruthy();

    // Change time range → triggers re-fetch
    apiAnalyticsV2Service.setTimeRangeFilter(timeFrameRangesParams('7d'));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('gio-loader')).toBeTruthy();

    // Second call
    req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url === analyticsUrl && r.params.get('type') === 'GROUP_BY');
    req.flush(fakeAnalyticsGroupBy({ values: { '200': 50 } }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('gio-chart-pie')).toBeTruthy();
  });
});
