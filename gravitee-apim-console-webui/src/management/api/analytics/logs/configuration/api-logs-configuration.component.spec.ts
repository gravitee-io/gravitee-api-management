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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiLogsConfigurationComponent } from './api-logs-configuration.component';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiLogsModule } from '../api-logs.module';

describe('ApiLogsConfigurationComponent', () => {
  const API_ID = 'my-api';
  const fakeUiRouter = { go: jest.fn() };
  let fixture: ComponentFixture<ApiLogsConfigurationComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, ApiLogsModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: fakeUiRouter },
      ],
    });

    fixture = TestBed.createComponent(ApiLogsConfigurationComponent);
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe('goToLoggingDashboard', () => {
    it('should redirect to logging dashboard', async () => {
      await loader
        .getHarness(MatButtonHarness.with({ selector: '[aria-label="Go to Logging dashboard"]' }))
        .then((button) => button.click());

      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.detail.analytics.logs.list');
    });
  });
});
