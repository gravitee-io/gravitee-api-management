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
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiRuntimeLogsModule } from './api-runtime-logs.module';
import { ApiRuntimeLogsComponent } from './api-runtime-logs.component';
import { ApiRuntimeLogsHarness } from './api-runtime-logs.component.harness';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioHttpTestingModule } from '../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../entities/management-api-v2';

describe('ApiRuntimeLogsComponent', () => {
  const apiId = 'apiId';
  let api: ApiV4;
  let fixture: ComponentFixture<ApiRuntimeLogsComponent>;
  let componentHarness: ApiRuntimeLogsHarness;

  const openLogSettingsButtonText = 'Open log settings';

  const initComponent = async (testApi: ApiV4) => {
    api = testApi;

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiRuntimeLogsModule, HttpClientTestingModule, MatIconTestingModule, GioHttpTestingModule],
      providers: [{ provide: UIRouterStateParams, useValue: { apiId: apiId } }],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsHarness);

    fixture.detectChanges();
  };

  describe('GIVEN the current page is the Runtime Logs page', () => {
    beforeEach(async () => {
      api = fakeApiV4({ id: apiId, analytics: { enabled: false } });
      await initComponent(api);
    });
    describe('WHEN the runtime logs are disabled', () => {
      it('THEN the disabled runtime log UI should be visible', async () => {
        expect(await componentHarness.getOpenLogSettingsButtonText()).toEqual(openLogSettingsButtonText);
      });
    });
  });
});
