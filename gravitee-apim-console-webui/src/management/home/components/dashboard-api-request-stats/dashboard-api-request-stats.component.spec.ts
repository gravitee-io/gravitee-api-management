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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { DashboardApiRequestStats, v4ApisRequestStats } from './dashboard-api-request-stats.component';
import { DashboardApiRequestStatsHarness } from './dashboard-api-request-stats.harness';

import { SUB_MILLISECOND_LABEL } from '../../../../shared/components/analytics-stats/analytics-stats.component';

describe('DashboardApiRequestStats', () => {
  let fixture: ComponentFixture<DashboardApiRequestStats>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, DashboardApiRequestStats],
    });

    fixture = TestBed.createComponent(DashboardApiRequestStats);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should display response times when values are non-zero', async () => {
    loadData({
      requestsPerSecond: 1.5,
      requestsTotal: 1000,
      responseMinTime: 5,
      responseMaxTime: 200,
      responseAvgTime: 42.5,
    });
    const harness = await loader.getHarness(DashboardApiRequestStatsHarness);

    expect(await harness.getMinResponseTime()).toContain('5.0 ms');
    expect(await harness.getMaxResponseTime()).toContain('200.0 ms');
    expect(await harness.getAverageResponseTime()).toContain('42.5 ms');
  });

  it('should display sub-millisecond label for zero min response time', async () => {
    loadData({
      requestsPerSecond: 10,
      requestsTotal: 50000,
      responseMinTime: 0,
      responseMaxTime: 150,
      responseAvgTime: 42,
    });
    const harness = await loader.getHarness(DashboardApiRequestStatsHarness);

    expect(await harness.getMinResponseTime()).toContain(SUB_MILLISECOND_LABEL);
  });

  it('should display dash for min response time when there are no requests', async () => {
    loadData({
      requestsPerSecond: 0,
      requestsTotal: 0,
      responseMinTime: 0,
      responseMaxTime: 0,
      responseAvgTime: 0,
    });
    const harness = await loader.getHarness(DashboardApiRequestStatsHarness);

    expect(await harness.getMinResponseTime()).toContain('-');
  });

  function loadData(data: v4ApisRequestStats) {
    fixture.componentInstance.data = data;
    fixture.detectChanges();
  }
});
