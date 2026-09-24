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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { PortalAuthenticationComponent } from './portal-authentication.component';
import { PortalAuthenticationHarness } from './portal-authentication.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';
import { PortalSettings } from '../../entities/portal/portalSettings';
import { fakePortalSettings } from '../../entities/portal/portalSettings.fixture';
import {
  fakeIdentityProviderActivation,
  fakeIdentityProviderListItem,
  IdentityProviderActivation,
  IdentityProviderListItem,
} from '../../entities/identity-provider';

const ALL_PERMISSIONS = [
  'organization-identity_provider-r',
  'environment-identity_provider_activation-r',
  'environment-identity_provider_activation-u',
  'environment-settings-r',
  'environment-settings-u',
];

describe('PortalAuthenticationComponent', () => {
  let fixture: ComponentFixture<PortalAuthenticationComponent>;
  let harness: PortalAuthenticationHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const google = fakeIdentityProviderListItem({ id: 'google', type: 'GOOGLE', name: 'Google', description: 'Google login' });
  const github = fakeIdentityProviderListItem({ id: 'github', type: 'GITHUB', name: 'GitHub', description: '' });
  const internalOidc = fakeIdentityProviderListItem({ id: 'internal', type: 'OIDC', name: 'Internal', enabled: false });

  async function init(permissions: string[] = ALL_PERMISSIONS): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [PortalAuthenticationComponent, GioTestingModule, NoopAnimationsModule, MatIconTestingModule],
      providers: [{ provide: GioTestingPermissionProvider, useValue: permissions }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PortalAuthenticationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = undefined;
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  }

  async function load(
    identityProviders: IdentityProviderListItem[],
    activations: IdentityProviderActivation[],
    settings: PortalSettings,
  ): Promise<void> {
    fixture.detectChanges();
    expectListIdentityProviders(identityProviders);
    expectListActivations(activations);
    expectGetSettings(settings);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    // The page is only stable once the data is loaded, so the harness can only be created afterwards
    harness ??= await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalAuthenticationHarness);
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('configuration', () => {
    it('should display the current authentication settings', async () => {
      await init();
      await load(
        [google],
        [fakeIdentityProviderActivation({ identityProvider: 'google' })],
        settingsWith({ forceLogin: true, localLogin: false }),
      );

      expect(await (await harness.getForceLoginToggle()).isChecked()).toBe(true);
      expect(await (await harness.getLocalLoginToggle()).isChecked()).toBe(false);
    });

    it('should save the authentication settings and preserve unrelated settings', async () => {
      await init();
      const settings = settingsWith({ forceLogin: false, localLogin: true });
      await load([google], [fakeIdentityProviderActivation({ identityProvider: 'google' })], settings);

      await (await harness.getForceLoginToggle()).toggle();
      expect(fixture.componentInstance.hasUnsavedChanges()).toBe(true);
      await harness.submit();

      expectGetSettings(settings);
      const saveRequest = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.baseURL}/settings` });
      expect(saveRequest.request.body).toEqual({
        ...settings,
        authentication: {
          ...settings.authentication,
          forceLogin: { ...settings.authentication.forceLogin, enabled: true },
          localLogin: { ...settings.authentication.localLogin, enabled: true },
        },
      });
      saveRequest.flush(saveRequest.request.body);
      expectEnvironmentSettingsReload();

      await load([google], [fakeIdentityProviderActivation({ identityProvider: 'google' })], saveRequest.request.body);
      expect(fixture.componentInstance.hasUnsavedChanges()).toBe(false);
    });

    it('should disable a setting provided by the system configuration', async () => {
      await init();
      const settings = settingsWith({ forceLogin: false, localLogin: true });
      settings.metadata = { readonly: ['portal.authentication.forceLogin.enabled'] };
      await load([google], [fakeIdentityProviderActivation({ identityProvider: 'google' })], settings);

      expect(await (await harness.getForceLoginToggle()).isDisabled()).toBe(true);
      expect(await (await harness.getLocalLoginToggle()).isDisabled()).toBe(false);
    });

    it('should disable the settings without environment settings update permission', async () => {
      await init(['organization-identity_provider-r', 'environment-identity_provider_activation-r', 'environment-settings-r']);
      await load(
        [google],
        [fakeIdentityProviderActivation({ identityProvider: 'google' })],
        settingsWith({ forceLogin: false, localLogin: true }),
      );

      expect(await (await harness.getForceLoginToggle()).isDisabled()).toBe(true);
      expect(await (await harness.getLocalLoginToggle()).isDisabled()).toBe(true);
    });

    it('should force the login form when no identity provider is activated', async () => {
      await init();
      const settings = settingsWith({ forceLogin: false, localLogin: false });
      await load([google], [], settings);

      expectGetSettings(settings);
      const saveRequest = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.baseURL}/settings` });
      expect(saveRequest.request.body.authentication.localLogin.enabled).toBe(true);
      saveRequest.flush(saveRequest.request.body);
      expectEnvironmentSettingsReload();
      await load([google], [], saveRequest.request.body);

      expect(await (await harness.getLocalLoginToggle()).isChecked()).toBe(true);
      expect(await (await harness.getLocalLoginToggle()).isDisabled()).toBe(true);
    });

    it('should update the login form state when the first identity provider is activated then deactivated', async () => {
      await init();
      const settings = settingsWith({ localLogin: true });
      await load([google], [], settings);
      expect(await (await harness.getLocalLoginToggle()).isDisabled()).toBe(true);
      expect(await harness.isLocalLoginLabelDisabled()).toBe(true);

      await harness.clickActivation('google');
      await (await rootLoader.getHarness(GioConfirmDialogHarness)).confirm();
      httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/identities` }).flush([]);
      await load([google], [fakeIdentityProviderActivation({ identityProvider: 'google' })], settings);

      expect(await (await harness.getLocalLoginToggle()).isDisabled()).toBe(false);
      expect(await harness.isLocalLoginLabelDisabled()).toBe(false);

      await harness.clickActivation('google');
      await (await rootLoader.getHarness(GioConfirmDialogHarness)).confirm();
      httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/identities` }).flush([]);
      await load([google], [], settings);

      expect(await (await harness.getLocalLoginToggle()).isDisabled()).toBe(true);
      expect(await harness.isLocalLoginLabelDisabled()).toBe(true);
    });
  });

  describe('identity providers', () => {
    it('should list the identity providers with their activation state', async () => {
      await init();
      await load([google, github], [fakeIdentityProviderActivation({ identityProvider: 'google' })], settingsWith({}));

      expect(await harness.getRows()).toEqual([
        { id: 'google', name: 'Google', description: 'Google login', activated: true },
        { id: 'github', name: 'GitHub', description: '', activated: false },
      ]);
    });

    it('should filter the identity providers with the search', async () => {
      await init();
      await load([google, github], [], settingsWith({ localLogin: true }));

      await harness.search('hub');

      expect((await harness.getRows()).map(row => row.id)).toEqual(['github']);
    });

    it('should display an empty message when there is no identity provider', async () => {
      await init();
      await load([], [], settingsWith({ localLogin: true }));

      expect(await harness.getEmptyMessage()).toBe('No identity providers to display.');
    });

    it('should activate an identity provider after confirmation', async () => {
      await init();
      const settings = settingsWith({});
      await load([google, github], [fakeIdentityProviderActivation({ identityProvider: 'google' })], settings);

      await harness.clickActivation('github');
      await (await rootLoader.getHarness(GioConfirmDialogHarness)).confirm();

      const updateRequest = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/identities` });
      expect(updateRequest.request.body).toEqual([{ identityProvider: 'google' }, { identityProvider: 'github' }]);
      updateRequest.flush([]);

      await load(
        [google, github],
        [fakeIdentityProviderActivation({ identityProvider: 'google' }), fakeIdentityProviderActivation({ identityProvider: 'github' })],
        settings,
      );
      expect((await harness.getRows()).map(row => row.activated)).toEqual([true, true]);
    });

    it('should deactivate an identity provider after confirmation', async () => {
      await init();
      const settings = settingsWith({});
      await load(
        [google, github],
        [fakeIdentityProviderActivation({ identityProvider: 'google' }), fakeIdentityProviderActivation({ identityProvider: 'github' })],
        settings,
      );

      await harness.clickActivation('google');
      await (await rootLoader.getHarness(GioConfirmDialogHarness)).confirm();

      const updateRequest = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/identities` });
      expect(updateRequest.request.body).toEqual([{ identityProvider: 'github' }]);
      updateRequest.flush([]);

      await load([google, github], [fakeIdentityProviderActivation({ identityProvider: 'github' })], settings);
    });

    it('should not update the activations when the confirmation is cancelled', async () => {
      await init();
      await load([google], [fakeIdentityProviderActivation({ identityProvider: 'google' })], settingsWith({}));

      await harness.clickActivation('google');
      await (await rootLoader.getHarness(GioConfirmDialogHarness)).cancel();

      httpTestingController.expectNone({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/identities` });
    });

    it('should not allow to activate an identity provider not allowed for portal authentication', async () => {
      await init();
      await load([google, internalOidc], [fakeIdentityProviderActivation({ identityProvider: 'google' })], settingsWith({}));

      expect(await harness.isActivationDisabled('google')).toBe(false);
      expect(await harness.isActivationDisabled('internal')).toBe(true);
      expect(await harness.getActivationTooltip('internal')).toBe(
        'Not allowed for portal authentication. Enable it in Platform → Authentication.',
      );
    });

    it('should hide the activation action without activation update permission', async () => {
      await init(['organization-identity_provider-r', 'environment-identity_provider_activation-r', 'environment-settings-u']);
      await load([google], [fakeIdentityProviderActivation({ identityProvider: 'google' })], settingsWith({}));

      expect(await harness.hasActivationAction('google')).toBe(false);
    });
  });

  function settingsWith({ forceLogin = false, localLogin = true }: { forceLogin?: boolean; localLogin?: boolean }): PortalSettings {
    const settings = fakePortalSettings();
    return {
      ...settings,
      metadata: { readonly: [] },
      authentication: {
        ...settings.authentication,
        forceLogin: { enabled: forceLogin },
        localLogin: { enabled: localLogin },
      },
    };
  }

  function expectListIdentityProviders(identityProviders: IdentityProviderListItem[]): void {
    httpTestingController
      .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/identities` })
      .flush(identityProviders);
  }

  function expectListActivations(activations: IdentityProviderActivation[]): void {
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/identities` }).flush(activations);
  }

  function expectGetSettings(settings: PortalSettings): void {
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/settings` }).flush(settings);
  }

  function expectEnvironmentSettingsReload(): void {
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/portal` }).flush({});
  }
});
