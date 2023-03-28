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
import { SimpleChange } from '@angular/core';

import { HealthAvailabilityTimeFrameModule } from './health-availability-time-frame.module';
import { HealthAvailabilityTimeFrameComponent } from './health-availability-time-frame.component';
import { HealthAvailabilityTimeFrameHarness } from './health-availability-time-frame.harness';

describe('HealthAvailabilityTimeFrameComponent', () => {
  let fixture: ComponentFixture<HealthAvailabilityTimeFrameComponent>;
  let pieChartHarness: HealthAvailabilityTimeFrameHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, HealthAvailabilityTimeFrameModule],
      providers: [],
    });
    fixture = TestBed.createComponent(HealthAvailabilityTimeFrameComponent);
    pieChartHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, HealthAvailabilityTimeFrameHarness);
  });

  it('should show message when no data', async () => {
    expect(await pieChartHarness.hasNoData()).toBeTruthy();
    expect(await pieChartHarness.displaysChart()).toBeFalsy();
  });

  it('should render chart', async () => {
    fixture.componentInstance.option = {
      timestamp: {
        start: 1679900320000,
        interval: 2000,
      },
      data: [100.0, 80.0],
    };

    fixture.componentInstance.ngOnChanges({
      option: new SimpleChange(null, fixture.componentInstance.option, false),
    });
    fixture.detectChanges();

    expect(await pieChartHarness.hasNoData()).toBeFalsy();
    expect(await pieChartHarness.displaysChart()).toBeTruthy();
  });
});
