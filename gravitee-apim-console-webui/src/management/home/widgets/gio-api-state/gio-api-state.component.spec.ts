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
import { SimpleChange } from '@angular/core';

import { GioApiStateModule } from './gio-api-state.module';
import { ApiStateData, GioApiStateComponent } from './gio-api-state.component';

import { GioChartPieHarness } from '../../../../shared/components/gio-chart-pie/gio-chart-pie.harness';

describe('GioApiStateComponent', () => {
  const data: ApiStateData = {
    values: {
      STOPPED: 30,
      STARTED: 81,
    },
  };

  let fixture: ComponentFixture<GioApiStateComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioApiStateModule],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GioApiStateComponent);
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
