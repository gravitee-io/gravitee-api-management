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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ApiAnalyticsFiltersBarComponent } from './api-analytics-filters-bar.component';
import { ApiAnalyticsFiltersBarHarness } from './api-analytics-filters-bar.component.harness';
import { buildV4TimeRangeParams } from './api-analytics-filters-bar.configuration';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { GioTestingModule } from '../../../../../../shared/testing';

describe('ApiAnalyticsFiltersBarComponent', () => {
  let fixture: ComponentFixture<ApiAnalyticsFiltersBarComponent>;
  let apiAnalyticsV2Service: ApiAnalyticsV2Service;
  let setTimeRangeFilterSpy: jest.SpyInstance;

  const initComponent = async (queryParams: Record<string, string> = {}) => {
    TestBed.configureTestingModule({
      imports: [ApiAnalyticsFiltersBarComponent, NoopAnimationsModule, GioTestingModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams } } },
        { provide: Router, useValue: { navigate: jest.fn(), initialNavigation: jest.fn() } },
      ],
    });

    await TestBed.compileComponents();
    apiAnalyticsV2Service = TestBed.inject(ApiAnalyticsV2Service);
    setTimeRangeFilterSpy = jest.spyOn(apiAnalyticsV2Service, 'setTimeRangeFilter');
    fixture = TestBed.createComponent(ApiAnalyticsFiltersBarComponent);
    fixture.detectChanges();
  };

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should apply default PRD period and emit matching TimeRangeParams', async () => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2025-06-10T14:00:00.000Z'));
    await initComponent();
    const expected = buildV4TimeRangeParams('24h');
    expect(setTimeRangeFilterSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        id: '24h',
        from: expected.from,
        to: expected.to,
        interval: expected.interval,
      }),
    );
  });

  it('should emit TimeRangeParams for the selected preset', async () => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2025-06-10T14:00:00.000Z'));
    await initComponent();
    setTimeRangeFilterSpy.mockClear();
    jest.useRealTimers();

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsFiltersBarHarness);
    const matSelect = await harness.getMatSelect();
    await matSelect.open();
    const options = await matSelect.getOptions({ text: 'Last 5 minutes' });
    await options[0].click();

    expect(setTimeRangeFilterSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        id: '5m',
        from: expect.any(Number),
        to: expect.any(Number),
        interval: expect.any(Number),
      }),
    );
    expect(setTimeRangeFilterSpy.mock.calls[0][0].to).toBeGreaterThan(setTimeRangeFilterSpy.mock.calls[0][0].from);
  });

  it('should re-emit TimeRangeParams when refresh is clicked', async () => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2025-06-10T14:00:00.000Z'));
    await initComponent();
    setTimeRangeFilterSpy.mockClear();
    jest.useRealTimers();

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsFiltersBarHarness);
    await harness.refresh();

    expect(setTimeRangeFilterSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        id: '24h',
        from: expect.any(Number),
        to: expect.any(Number),
        interval: expect.any(Number),
      }),
    );
  });
});
