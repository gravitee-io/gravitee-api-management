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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioConfirmDialogHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { IdentityProvidersComponent } from './identity-providers.component';
import { IdentityProvidersModule } from './identity-providers.module';

import { PortalSettings } from '../../../entities/portal/portalSettings';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import {
  fakeIdentityProviderActivation,
  fakeIdentityProviderListItem,
  IdentityProviderActivation,
  IdentityProviderListItem,
} from '../../../entities/identity-provider';

describe('IdentityProvidersComponent', () => {
  let fixture: ComponentFixture<IdentityProvidersComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const portalSettingsMock = {
    authentication: {
      forceLogin: {
        enabled: false,
      },
      localLogin: {
        enabled: true,
      },
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, IdentityProvidersModule, MatIconTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'environment-identity_provider_activation-u',
            'environment-identity_provider_activation-d',
            'environment-identity_provider_activation-c',
            'environment-identity_provider_activation-r',
            'environment-settings-u',
            'environment-settings-d',
            'environment-settings-c',
            'environment-settings-r',
          ],
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(IdentityProvidersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.autoDetectChanges(true);
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Idendtity providers configuration details', () => {
    it('should edit Idendtity providers configuration form', async () => {
      const identityProvider = [
        fakeIdentityProviderListItem({
          id: 'google',
          type: 'GOOGLE',
          name: 'Google',
        }),
        fakeIdentityProviderListItem({
          id: 'github',
          type: 'GITHUB',
          name: 'GitHub',
        }),
      ];

      expectListIdentityProviderRequest(identityProvider);
      expectListActivatedIdentityProviderRequest([fakeIdentityProviderActivation({ identityProvider: 'gravitee-am' })]);
      expectPortalSettingsGetRequest(portalSettingsMock);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const forceLoginToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName=enabled]' }));
      expect(await forceLoginToggle.isChecked()).toBe(false);
      await forceLoginToggle.toggle();

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      expectPortalSettingsGetRequest(portalSettingsMock);
      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        authentication: {
          forceLogin: {
            enabled: true,
          },
          localLogin: {
            enabled: true,
          },
        },
      });
    });
  });

  describe('Idendtity providers table', () => {
    it('should display Idendtity providers table', async () => {
      const identityProvider = [
        fakeIdentityProviderListItem({
          id: 'google',
          type: 'GOOGLE',
          name: 'Google',
        }),
        fakeIdentityProviderListItem({
          id: 'github',
          type: 'GITHUB',
          name: 'GitHub',
        }),
      ];

      expectListIdentityProviderRequest(identityProvider);
      expectListActivatedIdentityProviderRequest([fakeIdentityProviderActivation({ identityProvider: 'gravitee-am' })]);
      expectPortalSettingsGetRequest(portalSettingsMock);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#identityProviderTable' }));
      expect(await table.getCellTextByIndex()).toEqual([
        ['', 'google', 'Google', '', 'toggle_off'],
        ['', 'github', 'GitHub', '', 'toggle_off'],
      ]);
    });

    it('should activate identity provider', async () => {
      const identityProvider = [
        fakeIdentityProviderListItem({
          id: 'google',
          type: 'GOOGLE',
          name: 'Google',
        }),
        fakeIdentityProviderListItem({
          id: 'github',
          type: 'GITHUB',
          name: 'GitHub',
        }),
      ];

      expectListIdentityProviderRequest(identityProvider);
      expectListActivatedIdentityProviderRequest([fakeIdentityProviderActivation({ identityProvider: 'gravitee-am' })]);
      expectPortalSettingsGetRequest(portalSettingsMock);

      const activateIdentityProviderToggle = await loader.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Identity provider activation"]' }),
      );
      await activateIdentityProviderToggle.click();

      const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await confirmDialog.confirm();

      expectIdentityProviderPutRequest([]);
      expectListIdentityProviderRequest(identityProvider);
      expectListActivatedIdentityProviderRequest([fakeIdentityProviderActivation({ identityProvider: 'gravitee-am' })]);
      expectPortalSettingsGetRequest(portalSettingsMock);
    });
  });

  function expectListIdentityProviderRequest(identityProviders: IdentityProviderListItem[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/identities`,
        method: 'GET',
      })
      .flush(identityProviders);
  }

  function expectListActivatedIdentityProviderRequest(activatedIdentityProviders: IdentityProviderActivation[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/identities`,
        method: 'GET',
      })
      .flush(activatedIdentityProviders);
  }

  function expectPortalSettingsGetRequest(portalSettings: PortalSettings) {
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/settings` }).flush(portalSettings);
  }

  function expectIdentityProviderPutRequest(activatedIdentityProviders: IdentityProviderActivation[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/identities`,
        method: 'PUT',
      })
      .flush(activatedIdentityProviders);
  }
});
