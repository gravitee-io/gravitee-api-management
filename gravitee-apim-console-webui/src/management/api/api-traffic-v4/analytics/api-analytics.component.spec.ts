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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiAnalyticsComponent } from './api-analytics.component';
import { ApiAnalyticsHarness } from './api-analytics.component.harness';

import { GioTestingModule } from '../../../../shared/testing';

describe('ApiAnalyticsComponent', () => {
  let fixture: ComponentFixture<ApiAnalyticsComponent>;
  let componentHarness: ApiAnalyticsHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsComponent, NoopAnimationsModule, HttpClientTestingModule, MatIconTestingModule, GioTestingModule],
    }).compileComponents();

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiAnalyticsComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsHarness);
    fixture.detectChanges();
  });

  describe('GIVEN Analytics are disabled', () => {
    it('should display the empty panel', async () => {
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeTruthy();
    });
  });
});
