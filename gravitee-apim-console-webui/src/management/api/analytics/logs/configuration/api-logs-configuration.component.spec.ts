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
import { HttpTestingController } from '@angular/common/http/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { ApiLogsConfigurationComponent } from './api-logs-configuration.component';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiLogsModule } from '../api-logs.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { Api } from '../../../../../entities/api';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { GioFormCardGroupHarness } from '../../../../../shared/components/gio-form-card-group/gio-form-card-group.harness';

describe('ApiLogsConfigurationComponent', () => {
  const API_ID = 'my-api';
  const fakeUiRouter = { go: jest.fn() };
  let fixture: ComponentFixture<ApiLogsConfigurationComponent>;
  let component: ApiLogsConfigurationComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, ApiLogsModule, GioHttpTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: fakeUiRouter },
      ],
    });

    fixture = TestBed.createComponent(ApiLogsConfigurationComponent);
    fixture.detectChanges();
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('goToLoggingDashboard', () => {
    it('should redirect to logging dashboard', async () => {
      const api = fakeApi({
        id: API_ID,
      });
      expectApiGetRequest(api);

      await loader
        .getHarness(MatButtonHarness.with({ selector: '[aria-label="Go to Logging dashboard"]' }))
        .then((button) => button.click());

      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.detail.analytics.logs.list');
    });
  });

  describe('form tests', () => {
    it('should get the api and init the form with logs disabled', () => {
      const api = fakeApi({
        id: API_ID,
        proxy: {
          logging: {
            condition: '',
            mode: 'NONE',
            content: 'NONE',
            scope: 'NONE',
          },
        },
      });
      expectApiGetRequest(api);

      expect(component.logsConfigurationForm).toBeTruthy();
      expect(component.logsConfigurationForm.getRawValue()).toStrictEqual({
        enabled: false,
        condition: '',
        mode: 'CLIENT_PROXY',
        content: 'HEADERS_PAYLOADS',
        scope: 'REQUEST_RESPONSE',
      });
    });

    it('should get the api and init the form with logs enabled', () => {
      const api = fakeApi({
        id: API_ID,
        proxy: {
          logging: {
            condition: "{#request.headers['Content-Type'][0] === 'application/json'}",
            mode: 'CLIENT',
            content: 'HEADERS',
            scope: 'REQUEST',
          },
        },
      });
      expectApiGetRequest(api);

      expect(component.logsConfigurationForm).toBeTruthy();
      expect(component.logsConfigurationForm.getRawValue()).toStrictEqual({
        enabled: true,
        condition: "{#request.headers['Content-Type'][0] === 'application/json'}",
        mode: 'CLIENT',
        content: 'HEADERS',
        scope: 'REQUEST',
      });
    });

    it('should enable and then disable logging configuration', async () => {
      const api = fakeApi({
        id: API_ID,
        proxy: {
          logging: {
            condition: '',
            mode: 'NONE',
            content: 'NONE',
            scope: 'NONE',
          },
        },
      });
      expectApiGetRequest(api);

      const groups = await loader.getAllHarnesses(GioFormCardGroupHarness);
      for (const group of groups) {
        expect(await group.isDisabled()).toBe(true);
      }

      const toggle = await loader.getHarness(MatSlideToggleHarness);
      await toggle.toggle();
      fixture.detectChanges();

      for (const group of groups) {
        expect(await group.isDisabled()).toBe(false);
      }

      const modeGroup = await loader.getHarness(GioFormCardGroupHarness.with({ selector: '.logging-card__logging-modes__card-group' }));
      expect(await modeGroup.getSelectedValue()).toStrictEqual('CLIENT_PROXY');

      await modeGroup.select('CLIENT');
      expect(await modeGroup.getSelectedValue()).toStrictEqual('CLIENT');

      await toggle.toggle();

      expect(await modeGroup.getSelectedValue()).toBeUndefined();
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
