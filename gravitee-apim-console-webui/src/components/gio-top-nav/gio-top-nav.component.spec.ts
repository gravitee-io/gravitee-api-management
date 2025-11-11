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
import { of } from 'rxjs';
import { GioLicenseService } from '@gravitee/ui-particles-angular';

import { GioTopNavComponent } from './gio-top-nav.component';

import { Constants } from '../../entities/Constants';
import { TaskService } from '../../services-ngx/task.service';
import { UiCustomizationService } from '../../services-ngx/ui-customization.service';
import { EnvironmentSettingsService } from '../../services-ngx/environment-settings.service';

declare global {
  interface Window {
    pendo?: { isReady: () => boolean };
  }
}

const taskServiceMock = {
  getTasksAutoFetch: jest.fn(() => of({ page: { total_elements: 0 } })),
};

const uiCustomizationServiceMock = {
  getConsoleCustomization: jest.fn(() => of(undefined)),
};

const licenseServiceMock = {
  isOEM$: jest.fn(() => of(false)),
};

const environmentSettingsServiceMock = {
  get: jest.fn(),
};

const buildConstants = (overrides?: Partial<Constants>): Constants =>
  ({
    env: {
      // base URL pattern where {:envId} should be replaced
      baseURL: '/organizations/DEFAULT/environments/{:envId}',
    },
    org: {
      currentEnv: {
        id: 'DEFAULT',
      },
      settings: {
        management: {
          support: { enabled: true },
        },
      },
    },
    isOEM: false,
    customization: undefined as any,
    ...((overrides as any) ?? {}),
  }) as unknown as Constants;

describe('GioTopNavComponent', () => {
  let component: GioTopNavComponent;
  let fixture: ComponentFixture<GioTopNavComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [GioTopNavComponent],
      providers: [
        { provide: Constants, useValue: buildConstants() },
        { provide: TaskService, useValue: taskServiceMock },
        { provide: UiCustomizationService, useValue: uiCustomizationServiceMock },
        { provide: GioLicenseService, useValue: licenseServiceMock },
        { provide: EnvironmentSettingsService, useValue: environmentSettingsServiceMock },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GioTopNavComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should compute portalUrl without classic query when Portal Next is enabled', () => {
    // Given Portal Next enabled in environment settings
    environmentSettingsServiceMock.get.mockReturnValue(
      of({ portal: { url: 'https://portal.company.tld' }, portalNext: { access: { enabled: true } } }),
    );

    fixture.detectChanges();

    const constants = TestBed.inject(Constants);
    const expectedBase = constants.env.baseURL.replace('{:envId}', constants.org.currentEnv.id) + '/portal/redirect';
    expect(component.portalUrl).toBe(expectedBase);
    expect(component.isPortalNextEnabled).toBe(true);
  });

  it('should compute portalUrl and append ?version=classic when Portal Next is disabled', () => {
    // Given Portal Next disabled in environment settings
    environmentSettingsServiceMock.get.mockReturnValue(
      of({ portal: { url: 'https://portal.company.tld' }, portalNext: { access: { enabled: false } } }),
    );

    fixture.detectChanges();

    const constants = TestBed.inject(Constants);
    const expectedBase = constants.env.baseURL.replace('{:envId}', constants.org.currentEnv.id) + '/portal/redirect';
    expect(component.isPortalNextEnabled).toBe(false);
    expect(component.portalUrl).toBe(expectedBase + '?version=classic');
  });

  it('should keep portalUrl undefined if portal url is missing in settings', () => {
    // Given settings with no portal url
    environmentSettingsServiceMock.get.mockReturnValue(of({ portal: { url: '' }, portalNext: { access: { enabled: false } } }));

    fixture.detectChanges();

    // Then - despite Portal Next disabled, base portalUrl is undefined
    expect(component.portalUrl).toBeUndefined();
  });
});
