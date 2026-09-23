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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GioLicenseService, License } from '@gravitee/ui-particles-angular';
import { of, throwError } from 'rxjs';

import { PortalSettingsPageComponent } from './portal-settings-page.component';
import { PortalSettingsPageHarness } from './portal-settings-page.harness';

import { fakePortalSettings } from '../../entities/portal/portalSettings.fixture';
import { PortalSettings } from '../../entities/portal/portalSettings';
import { PortalSettingsService } from '../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioTestingModule } from '../../shared/testing';
import { GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';

describe('PortalSettingsPageComponent', () => {
  let fixture: ComponentFixture<PortalSettingsPageComponent>;
  let harness: PortalSettingsPageHarness;
  let portalSettingsService: { get: jest.Mock; save: jest.Mock };
  let snackBarService: { success: jest.Mock; error: jest.Mock };

  async function init(
    settings: PortalSettings,
    permissions = ['environment-settings-u', 'environment-settings-r'],
    license: License = { tier: 'galaxy', packs: [], features: [], isExpired: false },
  ): Promise<void> {
    let persistedSettings = settings;
    portalSettingsService = {
      get: jest.fn().mockImplementation(() => of(persistedSettings)),
      save: jest.fn().mockImplementation((updatedSettings: PortalSettings) => {
        persistedSettings = updatedSettings;
        return of(updatedSettings);
      }),
    };
    snackBarService = { success: jest.fn(), error: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [PortalSettingsPageComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        { provide: PortalSettingsService, useValue: portalSettingsService },
        { provide: SnackBarService, useValue: snackBarService },
        { provide: GioTestingPermissionProvider, useValue: permissions },
        { provide: GioLicenseService, useValue: { getLicense$: () => of(license) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PortalSettingsPageComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalSettingsPageHarness);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('loads the shared settings and exposes only enabled OpenAPI viewers', async () => {
    const settings = fakePortalSettings();
    await init(settings);

    expect(await harness.getApiKeyHeader()).toBe(settings.portal.apikeyHeader);
    expect(await harness.getPortalUrl()).toBe(settings.portal.url);
    expect(await harness.getKafkaSaslMechanisms()).toBe('PLAIN, SCRAM-SHA-256, SCRAM-SHA-512');
    expect(await (await harness.getRegistrationToggle()).isChecked()).toBe(true);
    expect(await (await harness.getAutomaticValidationToggle()).isChecked()).toBe(true);
    expect(await (await harness.getSwaggerViewer()).isDisabled()).toBe(true);
    expect(await (await harness.getRedocViewer()).isDisabled()).toBe(false);
    expect(await (await harness.getRedocViewer()).isChecked()).toBe(true);
  });

  it('loads the persisted Portal Next capabilities', async () => {
    const settings = fakePortalSettings();
    settings.portalNext.mtls.enabled = true;
    settings.portalNext.analytics.enabled = false;
    settings.portalNext.catalog.fuzzySearch.enabled = true;

    await init(settings);

    expect(await harness.hasPortalCapabilitiesCard()).toBe(true);
    expect(await (await harness.getMtlsToggle()).isChecked()).toBe(true);
    expect(await (await harness.getAnalyticsToggle()).isChecked()).toBe(false);
    expect(await (await harness.getFuzzySearchToggle()).isChecked()).toBe(true);
  });

  it.each([
    { label: 'mTLS', getToggle: (page: PortalSettingsPageHarness) => page.getMtlsToggle(), capability: 'mtls' },
    { label: 'analytics', getToggle: (page: PortalSettingsPageHarness) => page.getAnalyticsToggle(), capability: 'analytics' },
    {
      label: 'fuzzy search',
      getToggle: (page: PortalSettingsPageHarness) => page.getFuzzySearchToggle(),
      capability: 'fuzzySearch',
    },
  ])('updates $label independently and preserves all unrelated settings', async ({ getToggle, capability }) => {
    const settings = fakePortalSettings();
    settings.portalNext.mtls.enabled = false;
    settings.portalNext.analytics.enabled = false;
    settings.portalNext.catalog.fuzzySearch.enabled = false;
    await init(settings);

    await (await getToggle(harness)).toggle();
    await harness.submit();

    const savedSettings = portalSettingsService.save.mock.calls[0][0] as PortalSettings;
    expect(savedSettings.portalNext.access).toEqual(settings.portalNext.access);
    expect(savedSettings.portalNext.applications).toEqual(settings.portalNext.applications);
    expect(savedSettings.portalNext.banner).toEqual(settings.portalNext.banner);
    expect(savedSettings.portal).toEqual(settings.portal);
    expect(savedSettings.cors).toEqual(settings.cors);

    expect(savedSettings.portalNext.mtls.enabled).toBe(capability === 'mtls');
    expect(savedSettings.portalNext.analytics.enabled).toBe(capability === 'analytics');
    expect(savedSettings.portalNext.catalog.fuzzySearch.enabled).toBe(capability === 'fuzzySearch');
  });

  it('keeps Portal Next capabilities editable when Portal Next access is disabled', async () => {
    const settings = fakePortalSettings();
    settings.portalNext.access.enabled = false;

    await init(settings);

    expect(await (await harness.getMtlsToggle()).isDisabled()).toBe(false);
    expect(await (await harness.getAnalyticsToggle()).isDisabled()).toBe(false);
    expect(await (await harness.getFuzzySearchToggle()).isDisabled()).toBe(false);
  });

  it('resets Portal Next capabilities to their persisted values', async () => {
    const settings = fakePortalSettings();
    settings.portalNext.mtls.enabled = true;
    settings.portalNext.analytics.enabled = false;
    settings.portalNext.catalog.fuzzySearch.enabled = true;
    await init(settings);

    await (await harness.getMtlsToggle()).toggle();
    await (await harness.getAnalyticsToggle()).toggle();
    await (await harness.getFuzzySearchToggle()).toggle();
    expect(fixture.componentInstance.hasUnsavedChanges()).toBe(true);

    await harness.reset();

    expect(await (await harness.getMtlsToggle()).isChecked()).toBe(true);
    expect(await (await harness.getAnalyticsToggle()).isChecked()).toBe(false);
    expect(await (await harness.getFuzzySearchToggle()).isChecked()).toBe(true);
    expect(fixture.componentInstance.hasUnsavedChanges()).toBe(false);
  });

  it('hides Portal Next capabilities for an OSS license', async () => {
    await init(fakePortalSettings(), ['environment-settings-u', 'environment-settings-r'], {
      tier: 'oss',
      packs: [],
      features: [],
      isExpired: false,
    });

    expect(await harness.hasPortalCapabilitiesCard()).toBe(false);
  });

  it('preserves the automatic validation value when registration is disabled', async () => {
    await init(fakePortalSettings());
    const registrationToggle = await harness.getRegistrationToggle();
    const automaticValidationToggle = await harness.getAutomaticValidationToggle();

    await registrationToggle.toggle();

    expect(await automaticValidationToggle.isDisabled()).toBe(true);
    expect(await automaticValidationToggle.isChecked()).toBe(true);

    await registrationToggle.toggle();

    expect(await automaticValidationToggle.isDisabled()).toBe(false);
    expect(await automaticValidationToggle.isChecked()).toBe(true);
  });

  it('disables all controls when the user has read permission only', async () => {
    await init(fakePortalSettings(), ['environment-settings-r']);

    expect(await harness.isApiKeyHeaderDisabled()).toBe(true);
    expect(await harness.isPortalUrlDisabled()).toBe(true);
    expect(await harness.isKafkaSaslMechanismsDisabled()).toBe(true);
    expect(await (await harness.getRegistrationToggle()).isDisabled()).toBe(true);
    expect(await (await harness.getRedocViewer()).isDisabled()).toBe(true);
    expect(await (await harness.getMtlsToggle()).isDisabled()).toBe(true);
    expect(await (await harness.getAnalyticsToggle()).isDisabled()).toBe(true);
    expect(await (await harness.getFuzzySearchToggle()).isDisabled()).toBe(true);
  });

  it('respects property-level read-only metadata', async () => {
    const settings = fakePortalSettings({
      metadata: {
        readonly: [
          'portal.apikey.header',
          'portal.kafka.saslMechanisms',
          'portal.next.mtls.enabled',
          'portal.next.analytics.enabled',
          'portal.next.catalog.fuzzySearch.enabled',
        ],
      },
    });
    await init(settings);

    expect(await harness.isApiKeyHeaderDisabled()).toBe(true);
    expect(await harness.isPortalUrlDisabled()).toBe(false);
    expect(await harness.isKafkaSaslMechanismsDisabled()).toBe(true);
    expect(await (await harness.getMtlsToggle()).isDisabled()).toBe(true);
    expect(await (await harness.getAnalyticsToggle()).isDisabled()).toBe(true);
    expect(await (await harness.getFuzzySearchToggle()).isDisabled()).toBe(true);
  });

  it('merges edited values into the complete settings payload', async () => {
    const settings = fakePortalSettings();
    await init(settings);

    await harness.setApiKeyHeader('X-Custom-Api-Key');
    await harness.toggleKafkaSaslMechanism('SCRAM-SHA-256');
    await harness.toggleKafkaSaslMechanism('SCRAM-SHA-512');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBe(true);
    await harness.submit();

    expect(portalSettingsService.save).toHaveBeenCalledTimes(1);
    expect(portalSettingsService.save).toHaveBeenCalledWith({
      ...settings,
      portal: {
        ...settings.portal,
        apikeyHeader: 'X-Custom-Api-Key',
        kafkaSaslMechanisms: ['PLAIN'],
        userCreation: {
          ...settings.portal.userCreation,
          automaticValidation: {
            ...settings.portal.userCreation.automaticValidation,
          },
        },
      },
      openAPIDocViewer: {
        ...settings.openAPIDocViewer,
        openAPIDocType: {
          ...settings.openAPIDocViewer.openAPIDocType,
        },
      },
    });
    expect(portalSettingsService.get).toHaveBeenCalledTimes(1);
    expect(snackBarService.success).toHaveBeenCalledWith('Portal settings saved successfully.');
    expect(snackBarService.success).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.hasUnsavedChanges()).toBe(false);
  });

  it('keeps entered values and dirty state after a save failure', async () => {
    await init(fakePortalSettings());
    portalSettingsService.save.mockReturnValue(throwError(() => ({ error: { message: 'Save failed' } })));

    await harness.setApiKeyHeader('X-Unsaved-Key');
    await harness.submit();

    expect(await harness.getApiKeyHeader()).toBe('X-Unsaved-Key');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBe(true);
    expect(snackBarService.error).toHaveBeenCalledWith('Save failed');
  });

  it('shows a recoverable load error and retries the request', async () => {
    const settings = fakePortalSettings();
    portalSettingsService = {
      get: jest
        .fn()
        .mockReturnValueOnce(throwError(() => new Error('Load failed')))
        .mockReturnValueOnce(of(settings)),
      save: jest.fn(),
    };
    snackBarService = { success: jest.fn(), error: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [PortalSettingsPageComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        { provide: PortalSettingsService, useValue: portalSettingsService },
        { provide: SnackBarService, useValue: snackBarService },
        { provide: GioTestingPermissionProvider, useValue: ['environment-settings-u', 'environment-settings-r'] },
        {
          provide: GioLicenseService,
          useValue: { getLicense$: () => of({ tier: 'galaxy', packs: [], features: [], isExpired: false } satisfies License) },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PortalSettingsPageComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalSettingsPageHarness);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.hasLoadError()).toBe(true);

    await harness.retry();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.hasLoadError()).toBe(false);
    expect(await harness.getApiKeyHeader()).toBe(settings.portal.apikeyHeader);
    expect(portalSettingsService.get).toHaveBeenCalledTimes(2);
  });
});
