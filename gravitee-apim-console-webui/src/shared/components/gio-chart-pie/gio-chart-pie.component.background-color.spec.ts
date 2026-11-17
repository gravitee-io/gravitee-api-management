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

import { GioChartPieModule } from './gio-chart-pie.module';
import { GioChartPieComponent, GioChartPieInput } from './gio-chart-pie.component';
import { GioChartPieHarness } from './gio-chart-pie.harness';

import { provideHighchartsTesting } from '../../testing';

describe('GioChartPieComponent background color', () => {
  let fixture: ComponentFixture<GioChartPieComponent>;
  let loader: HarnessLoader;

  const input: GioChartPieInput[] = [{ label: 'pulsar-repro-slice', value: 1, color: '#2B72FB' }];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioChartPieModule],
      providers: [provideHighchartsTesting()],
    });
    fixture = TestBed.createComponent(GioChartPieComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.componentInstance.input = input;
  });

  it('renders the chart with the light color scheme, not the one of the browser or OS', async () => {
    fixture.detectChanges();

    const pieChart = await loader.getHarness(GioChartPieHarness);
    expect(await pieChart.displaysChart()).toBe(true);

    // Highcharts 13 resolves its default colors (background, borders, labels) from light-dark() CSS variables: unless
    // the chart's own container pins the light scheme, they follow the browser or OS dark appearance.
    const container: HTMLElement | null = fixture.nativeElement.querySelector('.highcharts-container');
    expect(container).not.toBeNull();
    expect(container.style.getPropertyValue('color-scheme')).toBe('light');
  });
});
