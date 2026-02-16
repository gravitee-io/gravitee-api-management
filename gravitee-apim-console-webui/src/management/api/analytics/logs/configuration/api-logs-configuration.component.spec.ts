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
import { HttpTestingController } from '@angular/common/http/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { GioFormSelectionInlineHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { ActivatedRoute } from '@angular/router';

import { ApiLogsConfigurationComponent } from './api-logs-configuration.component';

import { ApiLogsModule } from '../api-logs.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApiV2, fakeApiV2 } from '../../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiLogsConfigurationComponent', () => {
  const API_ID = 'my-api';
  let fixture: ComponentFixture<ApiLogsConfigurationComponent>;
  let component: ApiLogsConfigurationComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, ApiLogsModule, GioTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-log-u'],
        },
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

  describe('form tests', () => {
    it('should get the api and init the form with logs disabled', () => {
      const api = fakeApiV2({
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
      const api = fakeApiV2({
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
      const api = fakeApiV2({
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

      const groups = await loader.getAllHarnesses(GioFormSelectionInlineHarness);
      for (const group of groups) {
        expect(await group.isDisabled()).toBe(true);
      }

      const toggle = await loader.getHarness(MatSlideToggleHarness);
      await toggle.toggle();
      fixture.detectChanges();

      for (const group of groups) {
        expect(await group.isDisabled()).toBe(false);
      }

      const modeGroup = await loader.getHarness(
        GioFormSelectionInlineHarness.with({ selector: '.logging-card__logging-modes__card-group' }),
      );
      expect(await modeGroup.getSelectedValue()).toStrictEqual('CLIENT_PROXY');

      await modeGroup.select('CLIENT');
      expect(await modeGroup.getSelectedValue()).toStrictEqual('CLIENT');

      await toggle.toggle();

      expect(await modeGroup.getSelectedValue()).toBeUndefined();
    });

    it('should save logging configuration', async () => {
      const api = fakeApiV2({
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

      await loader.getHarness(MatSlideToggleHarness).then(toggle => toggle.toggle());
      fixture.detectChanges();

      const modeGroup = await loader.getHarness(
        GioFormSelectionInlineHarness.with({ selector: '.logging-card__logging-modes__card-group' }),
      );
      await modeGroup.select('CLIENT');
      expect(await modeGroup.getSelectedValue()).toStrictEqual('CLIENT');

      const scopeGroup = await loader.getHarness(
        GioFormSelectionInlineHarness.with({ selector: '.logging-card__scope-modes__card-group' }),
      );
      await scopeGroup.select('REQUEST');
      expect(await scopeGroup.getSelectedValue()).toStrictEqual('REQUEST');

      const contentGroup = await loader.getHarness(
        GioFormSelectionInlineHarness.with({ selector: '.logging-card__content-modes__card-group' }),
      );
      await contentGroup.select('HEADERS');
      expect(await contentGroup.getSelectedValue()).toStrictEqual('HEADERS');

      await loader.getHarness(GioSaveBarHarness).then(saveBar => saveBar.clickSubmit());

      expectApiGetRequest(api);
      expectApiPutRequest({
        ...api,
        proxy: { ...api.proxy, logging: { mode: 'CLIENT', content: 'HEADERS', scope: 'REQUEST' } },
      });
      expectApiGetRequest(api);
    });

    it('should handle error on save', async () => {
      const snackBarServiceSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
      const api = fakeApiV2({
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

      await loader.getHarness(MatSlideToggleHarness).then(toggle => toggle.toggle());
      fixture.detectChanges();

      await loader.getHarness(GioSaveBarHarness).then(saveBar => saveBar.clickSubmit());

      expectApiGetRequest(api);
      expectApiPutRequestError(api.id);
      expect(snackBarServiceSpy).toHaveBeenCalled();
    });
  });

  describe('kubernetes origin tests', () => {
    it('should disable the form when the origin is kubernetes', async () => {
      const api = fakeApiV2({
        id: API_ID,
        proxy: {
          logging: {
            condition: '',
            mode: 'CLIENT',
            content: 'PAYLOADS',
            scope: 'RESPONSE',
          },
        },
        definitionContext: {
          origin: 'KUBERNETES',
        },
      });
      expectApiGetRequest(api);

      const groups = await loader.getAllHarnesses(GioFormSelectionInlineHarness);
      for (const group of groups) {
        expect(await group.isDisabled()).toBe(true);
      }

      expect(await loader.getHarness(MatSlideToggleHarness).then(toggle => toggle.isDisabled())).toStrictEqual(true);
      expect(await loader.getHarness(MatInputHarness).then(input => input.isDisabled())).toStrictEqual(true);
    });
  });

  function expectApiGetRequest(api: ApiV2) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: ApiV2) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
    expect(req.request.body.proxy.logging).toStrictEqual(api.proxy.logging);
    req.flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequestError(apiId: string) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`, method: 'PUT' })
      .error(new ErrorEvent('error'));
    fixture.detectChanges();
  }
});
