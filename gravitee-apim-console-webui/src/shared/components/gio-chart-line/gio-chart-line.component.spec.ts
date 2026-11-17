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

import { GioChartLineModule } from './gio-chart-line.module';
import { GioChartLineComponent } from './gio-chart-line.component';
import { GioChartLineHarness } from './gio-chart-line.harness';

import { provideHighchartsTesting } from '../../testing';

describe('GioChartLineComponent', () => {
  let fixture: ComponentFixture<GioChartLineComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioChartLineModule],
      providers: [provideHighchartsTesting()],
    });
    fixture = TestBed.createComponent(GioChartLineComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should render chart with the light color scheme, not the one of the browser or OS', async () => {
    fixture.componentInstance.data = [{ name: '200', values: [10, 20, 30] }];
    fixture.componentInstance.options = { pointStart: 1728992010000, pointInterval: 30000 };
    fixture.detectChanges();

    const lineChart = await loader.getHarness(GioChartLineHarness);
    expect(await lineChart.displaysChart()).toBeTruthy();
    expect(await lineChart.getChartColorScheme()).toBe('light');
  });
});
