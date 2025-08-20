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
import { HarnessLoader } from '@angular/cdk/testing';

import { GioChartBarComponent } from './gio-chart-bar.component';
import { GioChartBarHarness } from './gio-chart-bar.harness';

describe('GioChartBarComponent', () => {
  let fixture: ComponentFixture<GioChartBarComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioChartBarComponent],
      providers: [],
    });
    fixture = TestBed.createComponent(GioChartBarComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should show message when no data', async () => {
    const barChart = await loader.getHarness(GioChartBarHarness);
    expect(await barChart.hasNoData()).toBeTruthy();
    expect(await barChart.displaysChart()).toBeFalsy();
  });

  it('should render chart when data is provided', async () => {
    fixture.componentInstance.data = [
      {
        name: 'Series 1',
        values: [10, 20, 30, 40, 50],
      },
    ];
    fixture.componentInstance.options = {
      categories: ['Jan', 'Feb', 'Mar', 'Apr', 'May'],
    };
    fixture.detectChanges();

    const barChart = await loader.getHarness(GioChartBarHarness);
    expect(await barChart.hasNoData()).toBeFalsy();
    expect(await barChart.displaysChart()).toBeTruthy();
  });
});
