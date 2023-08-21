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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiRuntimeLogsModule } from './api-runtime-logs.module';
import { ApiRuntimeLogsComponent } from './api-runtime-logs.component';
import { ApiRuntimeLogsHarness } from './api-runtime-logs.component.harness';

import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING } from '../../../shared/testing';

describe('ApiRuntimeLogsComponent', () => {
  let fixture: ComponentFixture<ApiRuntimeLogsComponent>;
  let componentHarness: ApiRuntimeLogsHarness;

  const API_ID = 'apiId';
  const runtimeLogsTabTitle = 'Runtime Logs';
  const settingsTabTitle = 'Settings';

  const initComponent = async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiRuntimeLogsModule, HttpClientTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: 'Constants', useValue: CONSTANTS_TESTING },
      ],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsHarness);

    fixture.detectChanges();
  };

  describe('GIVEN the current page is the Runtime Logs page', () => {
    beforeEach(async () => {
      await initComponent();
    });
    describe('WHEN the page has loaded', () => {
      it('THEN the Runtime Logs tab should be visible', async () => {
        expect(await componentHarness.readRuntimeLogsTabLabel()).toEqual(runtimeLogsTabTitle);
      });
      it('THEN the Settings tab should be visible', async () => {
        expect(await componentHarness.readSettingsTabLabel()).toEqual(settingsTabTitle);
      });
    });
  });
});
