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
import { SimpleChange } from '@angular/core';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { GioApiResponseStatusModule } from './gio-api-response-status.module';
import { ApiResponseStatusData, GioApiResponseStatusComponent } from './gio-api-response-status.component';

import { GioChartPieHarness } from '../../../../shared/components/gio-chart-pie/gio-chart-pie.harness';

describe('GioApiResponseStatusComponent', () => {
  const data: ApiResponseStatusData = {
    values: {
      '100.0-200.0': 0,
      '200.0-300.0': 5,
      '300.0-400.0': 0,
      '400.0-500.0': 0,
      '500.0-600.0': 0,
    },
    metadata: {
      '300.0-400.0': {
        name: '300.0-400.0',
        order: '2',
      },
      '100.0-200.0': {
        name: '100.0-200.0',
        order: '0',
      },
      '200.0-300.0': {
        name: '200.0-300.0',
        order: '1',
      },
      '400.0-500.0': {
        name: '400.0-500.0',
        order: '3',
      },
      '500.0-600.0': {
        name: '500.0-600.0',
        order: '4',
      },
    },
  };

  let fixture: ComponentFixture<GioApiResponseStatusComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioApiResponseStatusModule],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GioApiResponseStatusComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    fixture.componentInstance.data = data;
    fixture.componentInstance.ngOnChanges({
      data: new SimpleChange(null, data, true),
    });
    fixture.detectChanges();
  });

  it('should init', async () => {
    const chartPie = await loader.getHarness(GioChartPieHarness);

    expect(await chartPie.displaysChart()).toBeTruthy();
  });
});
