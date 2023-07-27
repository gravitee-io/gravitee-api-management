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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { GioLicenseTestingModule } from '@gravitee/ui-particles-angular';

import { ClientRegistrationProvidersComponent } from './client-registration-providers.component';
import { ClientRegistrationProvidersModule } from './client-registration-providers.module';

import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { fakeClientRegistrationProvider } from '../../../entities/client-registration-provider/clientRegistrationProvider.fixture';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { PortalSettings } from '../../../entities/portal/portalSettings';

describe('ClientRegistrationProviders', () => {
  const fakeAjsState = {
    go: jest.fn(),
  };
  const providers = [fakeClientRegistrationProvider(), fakeClientRegistrationProvider()];
  let httpTestingController: HttpTestingController;

  const settings: PortalSettings = {
    metadata: {
      readonly: ['application.types.simple.enabled', 'application.types.web.enabled', 'application.types.backend_to_backend.enabled'],
    },
    application: {
      registration: {
        enabled: true,
      },
      types: {
        simple: {
          enabled: false,
        },
        browser: {
          enabled: false,
        },
        web: {
          enabled: true,
        },
        native: {
          enabled: true,
        },
        backend_to_backend: {
          enabled: false,
        },
      },
    },
  };

  let fixture: ComponentFixture<ClientRegistrationProvidersComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        GioHttpTestingModule,
        MatIconTestingModule,
        GioLicenseTestingModule.with(true),
        MatDialogModule,
        ClientRegistrationProvidersModule,
      ],
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
        { provide: UIRouterStateParams, useValue: {} },
        {
          provide: 'Constants',
          useValue: CONSTANTS_TESTING,
        },
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: () => true,
          },
        },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ClientRegistrationProvidersComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers`,
      })
      .flush({ data: providers });
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
      })
      .flush(settings);
    fixture.detectChanges();
    expect(fixture.componentInstance.isLoadingData).toBeFalsy();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should init with some readonly fields', async () => {
    expect(loader).toBeTruthy();
    const tableHarness = await loader.getHarness(MatTableHarness);
    const rows = await tableHarness.getRows();
    expect(rows.length).toEqual(2);

    await checkToggle([
      {
        dataTestId: 'registrationEnabled',
        value: settings.application.registration.enabled,
        isDisabled: settings.metadata.readonly.includes('application.registration.enabled'),
      },
      {
        dataTestId: 'typesSimpleEnabled',
        value: settings.application.types.simple.enabled,
        isDisabled: settings.metadata.readonly.includes('application.types.simple.enabled'),
      },
      {
        dataTestId: 'typesBrowserEnabled',
        value: settings.application.types.browser.enabled,
        isDisabled: settings.metadata.readonly.includes('application.types.browser.enabled'),
      },
      {
        dataTestId: 'typesWebEnabled',
        value: settings.application.types.web.enabled,
        isDisabled: settings.metadata.readonly.includes('application.types.web.enabled'),
      },
      {
        dataTestId: 'typesNativeEnabled',
        value: settings.application.types.native.enabled,
        isDisabled: settings.metadata.readonly.includes('application.types.native.enabled'),
      },
      {
        dataTestId: 'typesBackendToBackendEnabled',
        value: settings.application.types.backend_to_backend.enabled,
        isDisabled: settings.metadata.readonly.includes('application.types.backend_to_backend.enabled'),
      },
    ]);
  });

  async function checkToggle(toggles: { dataTestId: string; value: boolean; isDisabled: boolean }[]) {
    for (const toggle of toggles) {
      const toggleHarness = await loader.getHarness(MatSlideToggleHarness.with({ selector: `[data-testid="${toggle.dataTestId}"]` }));
      expect(await toggleHarness.isDisabled()).toEqual(toggle.isDisabled);
      expect(await toggleHarness.isChecked()).toEqual(toggle.value);
    }
  }
});
