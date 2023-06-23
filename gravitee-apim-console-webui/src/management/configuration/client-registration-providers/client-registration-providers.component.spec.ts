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
import { snakeCase } from 'lodash';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ClientRegistrationProvidersComponent } from './client-registration-providers.component';
import { ClientRegistrationProvidersModule } from './client-registration-providers.module';

import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { fakeClientRegistrationProvider } from '../../../entities/client-registration-provider/clientRegistrationProvider.fixture';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { GioLicenseTestingModule } from '../../../shared/testing/gio-license.testing.module';

describe('ClientRegistrationProviders', () => {
  const fakeAjsState = {
    go: jest.fn(),
  };
  const providers = [fakeClientRegistrationProvider(), fakeClientRegistrationProvider()];
  let httpTestingController: HttpTestingController;

  const settings = {
    application: {
      registration: {
        enabled: true,
      },
      types: {
        simple: {
          enabled: false,
        },
        browser: {
          enabled: true,
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
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ClientRegistrationProvidersComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.componentInstance.isReadonly = jest.fn().mockReturnValue(false);
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

  it('should init without readonly fields', async () => {
    expect(loader).toBeTruthy();
    const tableHarness = await loader.getHarness(MatTableHarness);
    const rows = await tableHarness.getRows();
    expect(rows.length).toEqual(2);

    await checkToggle(
      ['registrationEnabled', 'typesSimpleEnabled', 'typesBrowserEnabled', 'typesWebEnabled', 'typesNativeEnabled', 'typesBackendEnabled'],
      false,
    );
  });

  it('should init with readonly fields', async () => {
    fixture.componentInstance.isReadonly = jest.fn().mockReturnValue(true);
    expect(loader).toBeTruthy();
    const tableHarness = await loader.getHarness(MatTableHarness);
    const rows = await tableHarness.getRows();
    expect(rows.length).toEqual(2);

    await checkToggle(
      ['registrationEnabled', 'typesSimpleEnabled', 'typesBrowserEnabled', 'typesWebEnabled', 'typesNativeEnabled', 'typesBackendEnabled'],
      false,
    );
  });

  async function checkToggle(ids: string[], areDisabled: boolean) {
    for (const id of ids) {
      const toggleHarness = await loader.getHarness(MatSlideToggleHarness.with({ selector: `[data-testid="${id}"]` }));
      expect(await toggleHarness.isDisabled()).toEqual(areDisabled);
      expect(await toggleHarness.isChecked()).toEqual(settings.application[snakeCase(id).replace(/ /g, '.')] ?? false);
    }
  }
});
