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
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ReporterSettingsComponent } from './reporter-settings.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

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
          useValue: { params: of({ apiId: API_ID }) },
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
      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness);

      expect(await toggle.isDisabled()).toEqual(true);
      expect(await toggle.isChecked()).toEqual(true);
    });

    it('should disable toggle when not V4 API', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V2', 'HTTP', API_ID, {
        enabled: true,
      });
      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness);

      expect(await toggle.isDisabled()).toEqual(true);
      expect(await toggle.isChecked()).toEqual(true);
    });

    it('should disable toggle when not NATIVE API', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'HTTP', API_ID, {
        enabled: true,
      });
      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness);

      expect(await toggle.isDisabled()).toEqual(true);
      expect(await toggle.isChecked()).toEqual(true);
    });

    it('should not be checked when analytics is null', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, null);
      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness);

      expect(await toggle.isDisabled()).toEqual(false);
      expect(await toggle.isChecked()).toEqual(false);
    });

    it('should enable toggle when analytics is enabled', async () => {
      await init(['api-definition-r', 'api-definition-u']);
      fixture.detectChanges();
      expectGetAPI('V4', 'NATIVE', API_ID, {
        enabled: true,
      });
      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness);

      expect(await toggle.isDisabled()).toEqual(false);
      expect(await toggle.isChecked()).toEqual(true);
    });
  });

  function expectGetAPI(definitionVersion: string, type: string, apiId: string, analytics: any) {
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`)
      .flush({ definitionVersion: definitionVersion, type: type, analytics: analytics });
  }
});
