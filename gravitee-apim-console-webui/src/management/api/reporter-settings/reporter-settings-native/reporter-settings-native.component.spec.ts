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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatCardHarness } from '@angular/material/card/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ReporterSettingsComponent } from '../reporter-settings.component';

describe('ReporterSettingsComponent', () => {
  const API_ID = 'xyz-abc-pqr';
  let fixture: ComponentFixture<ReporterSettingsComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async (permissions: string[]) => {
    await TestBed.configureTestingModule({
      imports: [ReporterSettingsComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { apiId: API_ID } } },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ReporterSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('enable event metrics', () => {
    it('should disable toggle when not sufficient permission', async () => {
      await init(['api-definition-r']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, {
        enabled: true,
      });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const enabledToggle = toggles[0];
      const reporterMetricsToggle = toggles[1];

      expect(await enabledToggle.isDisabled()).toEqual(true);
      expect(await enabledToggle.isChecked()).toEqual(true);
      expect(await reporterMetricsToggle.isDisabled()).toEqual(true);
    });

    it('should disable toggle when not V4 API', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V2', 'HTTP', API_ID, {
        enabled: true,
      });
      const emptyState = await harnessLoader.getHarness(MatCardHarness);

      expect(await emptyState.getText()).toContain('The reporter settings are not available for your API.');
    });

    it('should not be checked when analytics is null', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, null);
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const enabledToggle = toggles[0];

      expect(await enabledToggle.isDisabled()).toEqual(false);
      expect(await enabledToggle.isChecked()).toEqual(false);
    });

    it('should enable toggle when analytics is enabled', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, {
        enabled: true,
      });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const enabledToggle = toggles[0];

      expect(await enabledToggle.isDisabled()).toEqual(false);
      expect(await enabledToggle.isChecked()).toEqual(true);
    });
  });

  describe('connection-metrics reporter toggle', () => {
    it('should default reporter-metrics toggle to checked when analytics is null', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, null);
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const reporterMetricsToggle = toggles[1];

      expect(await reporterMetricsToggle.isChecked()).toEqual(true);
    });

    it('should reflect reporter-metrics false from api', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, reporterMetricsEnabled: false });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const reporterMetricsToggle = toggles[1];

      expect(await reporterMetricsToggle.isChecked()).toEqual(false);
    });

    it('should remain enabled when analytics is off', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, reporterMetricsEnabled: true });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[0].uncheck();

      expect(await toggles[1].isDisabled()).toEqual(false);
    });

    it('should include reporterMetricsEnabled in submit payload', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, reporterMetricsEnabled: true }, true);
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[1].uncheck();

      const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, reporterMetricsEnabled: true }, true);

      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      expect(req.request.body.analytics.reporterMetricsEnabled).toEqual(false);
      req.flush(req.request.body);
    });
  });

  describe('OpenTelemetry tracing toggles', () => {
    it('should disable tracing toggles when analytics is disabled', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: false });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const tracingEnabledToggle = toggles[2];
      const tracingVerboseToggle = toggles[3];

      expect(await tracingEnabledToggle.isDisabled()).toEqual(true);
      expect(await tracingVerboseToggle.isDisabled()).toEqual(true);
    });

    it('should enable tracing-enabled toggle when analytics is enabled', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const tracingEnabledToggle = toggles[2];

      expect(await tracingEnabledToggle.isDisabled()).toEqual(false);
      expect(await tracingEnabledToggle.isChecked()).toEqual(false);
    });

    it('should disable verbose when tracing is not enabled', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const tracingVerboseToggle = toggles[3];

      expect(await tracingVerboseToggle.isDisabled()).toEqual(true);
    });

    it('should enable verbose when tracing is enabled', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: true, verbose: false } });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const tracingVerboseToggle = toggles[3];

      expect(await tracingVerboseToggle.isDisabled()).toEqual(false);
      expect(await tracingVerboseToggle.isChecked()).toEqual(false);
    });

    it('should reflect tracing verbose checked state', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: true, verbose: true } });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const tracingVerboseToggle = toggles[3];

      expect(await tracingVerboseToggle.isChecked()).toEqual(true);
    });

    it('should disable tracing toggles when read-only', async () => {
      await init(['api-definition-r']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: true, verbose: true } });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      const tracingEnabledToggle = toggles[2];
      const tracingVerboseToggle = toggles[3];

      expect(await tracingEnabledToggle.isDisabled()).toEqual(true);
      expect(await tracingVerboseToggle.isDisabled()).toEqual(true);
    });

    it('should disable tracing toggles when analytics enabled is toggled off', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: true, verbose: true } });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[0].uncheck();

      expect(await toggles[2].isDisabled()).toEqual(true);
      expect(await toggles[3].isDisabled()).toEqual(true);
    });

    it('should enable tracing-enabled toggle when analytics enabled is toggled on', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: false });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[0].check();

      expect(await toggles[2].isDisabled()).toEqual(false);
    });

    it('should disable verbose when tracing-enabled is toggled off', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: true, verbose: true } });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[2].uncheck();

      expect(await toggles[3].isDisabled()).toEqual(true);
    });

    it('should enable verbose when tracing-enabled is toggled on', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } });
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[2].check();

      expect(await toggles[3].isDisabled()).toEqual(false);
    });

    it('should submit tracing settings via API update', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } }, true);
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[2].check();
      await toggles[3].check();

      const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      // submit() fetches the latest API state before issuing the PUT
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } }, true);

      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      expect(req.request.body.analytics.tracing).toEqual({ enabled: true, verbose: true });
      expect(req.request.body.analytics.reporterMetricsEnabled).toEqual(true);
      req.flush(req.request.body);
    });

    it('should dismiss the save bar and show a success snackbar after saving', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } }, true);
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[2].check();

      const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toEqual(true);

      const snackBarService = TestBed.inject(SnackBarService);
      const successSpy = jest.spyOn(snackBarService, 'success').mockImplementation(() => undefined);

      await saveBar.clickSubmit();

      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } }, true);
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      req.flush(req.request.body);
      fixture.detectChanges();

      expect(successSpy).toHaveBeenCalledWith('Configuration successfully saved!');
      expect(await saveBar.isVisible()).toEqual(false);
    });

    it('should show an error snackbar when save fails', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } }, true);
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[2].check();

      const snackBarService = TestBed.inject(SnackBarService);
      const errorSpy = jest.spyOn(snackBarService, 'error');

      const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } }, true);
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      req.flush({ message: 'Validation failed' }, { status: 400, statusText: 'Bad Request' });

      expect(errorSpy).toHaveBeenCalledWith('Validation failed');
    });

    it('should show a fallback error snackbar when save fails without a body', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } }, true);
      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);

      await toggles[2].check();

      const snackBarService = TestBed.inject(SnackBarService);
      const errorSpy = jest.spyOn(snackBarService, 'error');

      const saveBar = await harnessLoader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      expectGetAPI('V4', 'NATIVE', API_ID, { enabled: true, tracing: { enabled: false, verbose: false } }, true);
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      req.error(new ProgressEvent('error'));

      expect(errorSpy).toHaveBeenCalledWith('Failed to save analytics settings');
    });
  });

  function expectGetAPI(definitionVersion: string, type: string, apiId: string, analytics: any, includeId = false) {
    const body: any = { definitionVersion, type, analytics };
    if (includeId) {
      body.id = apiId;
    }
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`).flush(body);
  }
});
