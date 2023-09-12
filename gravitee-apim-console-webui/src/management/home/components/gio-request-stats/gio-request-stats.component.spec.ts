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

import { GioRequestStatsModule } from './gio-request-stats.module';
import { GioRequestStatsHarness } from './gio-request-stats.harness';
import { GioRequestStatsComponent } from './gio-request-stats.component';

import { AnalyticsStatsResponse } from '../../../../entities/analytics/analyticsResponse';

describe('GioRequestStatsComponent', () => {
  let fixture: ComponentFixture<GioRequestStatsComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioRequestStatsModule],
      providers: [],
    });

    fixture = TestBed.createComponent(GioRequestStatsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should show given data', async () => {
    loadData({
      min: 0.02336,
      max: 23009.29732,
      avg: 8.4363,
      rps: 1.2712334,
      rpm: 76.274004,
      rph: 4576.44024,
      count: 332981092,
      sum: 43788.0,
    });
    const harness = await loader.getHarness(GioRequestStatsHarness);
    expect(await harness.getMin()).toEqual('0.02 ms');
    expect(await harness.getMax()).toEqual('23,009.3 ms');
    expect(await harness.getAverage()).toEqual('8.44 ms');
    expect(await harness.getRequestsPerSecond()).toEqual('1.3');
    expect(await harness.getTotalRequests()).toEqual('332.98M');
  });

  function loadData(data: AnalyticsStatsResponse) {
    fixture.componentInstance.data = data;
    fixture.detectChanges();
  }
});
