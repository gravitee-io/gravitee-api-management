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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiRuntimeLogsSettingsModule } from './api-runtime-logs-settings.module';
import { ApiRuntimeLogsSettingsComponent } from './api-runtime-logs-settings.component';
import { ApiRuntimeLogsSettingsHarness } from './api-runtime-logs-settings.harness';

import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { User } from '../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ApiV4 } from '../../../../entities/management-api-v2';

describe('ApiRuntimeLogsSettingsComponent', () => {
  const API_ID = 'apiId';
  const testApi: ApiV4 = {
    id: API_ID,
    definitionVersion: 'V4',
    type: 'MESSAGE',
    name: 'test',
    apiVersion: '1',
    analytics: {
      enabled: true,
    },
  };
  let fixture: ComponentFixture<ApiRuntimeLogsSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiRuntimeLogsSettingsHarness;

  const initComponent = async (api: ApiV4 = testApi) => {
    const currentUser = new User();
    currentUser.userPermissions = ['api-definition-u'];

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiRuntimeLogsSettingsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: { go: jest.fn() } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    });

    fixture = TestBed.createComponent(ApiRuntimeLogsSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsSettingsHarness);
    fixture.detectChanges();

    expectApiGetRequest(api);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('init tests', () => {
    it('should init the component with disabled logs', async () => {
      await initComponent({ ...testApi, analytics: { enabled: false } });
      await expect(componentHarness.areLogsEnabled()).resolves.toBe(false);
      await expect(componentHarness.getLogsBanner()).rejects.toThrow(/Failed to find element/);
    });

    it('should init the component with enabled logs', async () => {
      await initComponent();
      await expect(componentHarness.areLogsEnabled()).resolves.toBe(true);
      await expect(componentHarness.getLogsBanner()).resolves.toBeTruthy();
    });
  });

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
  }
});
