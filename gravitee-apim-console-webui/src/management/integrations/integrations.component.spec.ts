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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestElement } from '@angular/cdk/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { of } from 'rxjs';
import { GioLicenseService, License } from '@gravitee/ui-particles-angular';

import { IntegrationsComponent } from './integrations.component';
import { IntegrationsHarness } from './integrations.harness';
import { Integration, IntegrationResponse } from './integrations.model';
import { IntegrationsModule } from './integrations.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { fakeIntegration } from '../../entities/integrations/integration.fixture';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';
import { ConsoleSettings } from '../../entities/consoleSettings';
import { Constants } from '../../entities/Constants';

describe('IntegrationsComponent', () => {
  let fixture: ComponentFixture<IntegrationsComponent>;
  let componentHarness: IntegrationsHarness;
  let httpTestingController: HttpTestingController;

  const init = async (
    permissions: GioTestingPermission = [
      'environment-integration-u',
      'environment-integration-d',
      'environment-integration-c',
      'environment-integration-r',
    ],
    licence: License = {
      tier: 'universe',
      packs: ['', ''],
      features: [''],
      isExpired: false,
    },
    consoleSettings: ConsoleSettings = { federation: { enabled: true } },
  ) => {
    await TestBed.configureTestingModule({
      declarations: [IntegrationsComponent],
      imports: [IntegrationsModule, GioTestingModule, BrowserAnimationsModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [...permissions],
        },
        {
          provide: GioLicenseService,
          useValue: {
            openDialog: jest.fn(),
            getLicense$: () => of(licence),
          },
        },
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(IntegrationsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationsHarness);
    fixture.componentInstance.filters = {
      pagination: { index: 1, size: 10 },
      searchTerm: '',
    };
    fixture.detectChanges();

    expectConsoleSettingsGetRequest(consoleSettings);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('create integration with permissions ', () => {
    beforeEach(() => {
      init();
    });

    it('should allow create integration', async () => {
      const fakeIntegrations: Integration[] = [fakeIntegration(), fakeIntegration(), fakeIntegration()];

      expectIntegrationGetRequest(fakeIntegrations);
      expect(await componentHarness.getCreateIntegrationButton()).toBeTruthy();
    });
  });

  describe('without permissions ', () => {
    beforeEach(() => {
      init([]);
    });

    it('should not allow create integration', async () => {
      const fakeIntegrations: Integration[] = [fakeIntegration(), fakeIntegration(), fakeIntegration()];

      expectIntegrationGetRequest(fakeIntegrations);
      expect(await componentHarness.getCreateIntegrationButton()).toBeNull();
    });
  });

  describe('table', () => {
    beforeEach(() => {
      init();
    });

    it('should display correct number of rows', async () => {
      const fakeIntegrations: Integration[] = [fakeIntegration(), fakeIntegration(), fakeIntegration()];

      expectIntegrationGetRequest(fakeIntegrations);

      const rows = await componentHarness.rowsNumber();
      expect(rows).toEqual(fakeIntegrations.length);
    });
  });

  describe('pagination', () => {
    beforeEach(() => {
      init();
    });

    it('should request proper url', async () => {
      const fakeIntegrations: Integration[] = [fakeIntegration(), fakeIntegration(), fakeIntegration(), fakeIntegration()];

      expectIntegrationGetRequest(fakeIntegrations, 1, 10);
      const pagination: MatPaginatorHarness = await componentHarness.getPagination();

      await pagination.setPageSize(5);
      expectIntegrationGetRequest(fakeIntegrations, 1, 5);
      pagination.getPageSize().then((value) => {
        expect(value).toEqual(5);
      });

      await pagination.setPageSize(25);
      expectIntegrationGetRequest(fakeIntegrations, 1, 25);
      pagination.getPageSize().then((value) => {
        expect(value).toEqual(25);
      });
    });
  });

  describe('free tier info banners', (): void => {
    beforeEach((): void => {
      init(['environment-integration-u', 'environment-integration-d', 'environment-integration-c', 'environment-integration-r'], {
        tier: 'oss',
        packs: [],
        features: [],
        isExpired: false,
      });
    });

    it('should not request for integrations', async (): Promise<void> => {
      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations?page=1&perPage=10`);
    });

    it('should display license banner', async (): Promise<void> => {
      const licenceBanner: TestElement = await componentHarness.getLicenceBanner();
      expect(licenceBanner).toBeTruthy();
    });
  });

  describe('enterprise license info banners', (): void => {
    beforeEach((): void => {
      init(['environment-integration-u', 'environment-integration-d', 'environment-integration-c', 'environment-integration-r'], {
        tier: 'universe',
        packs: [],
        features: [],
        isExpired: false,
      });
    });

    it('should not display license banner', async (): Promise<void> => {
      expectIntegrationGetRequest();
      const licenceBanner = await componentHarness.getLicenceBanner();
      expect(licenceBanner).toBeNull();
    });
  });

  describe('module not activated info banners', (): void => {
    beforeEach((): void => {
      init(
        ['environment-integration-u', 'environment-integration-d', 'environment-integration-c', 'environment-integration-r'],
        {
          tier: 'universe',
          packs: [],
          features: [],
          isExpired: false,
        },
        { federation: { enabled: false } },
      );
    });

    it('should not display license banner', async (): Promise<void> => {
      expectIntegrationGetRequest();
      const licenceBanner = await componentHarness.getLicenceBanner();
      expect(licenceBanner).toBeNull();
    });
  });

  describe('no-integrations-banner', (): void => {
    beforeEach(() => {
      init();
    });

    it('should be visible when no integrations', async (): Promise<void> => {
      expectIntegrationGetRequest([]);
      const banner: TestElement = await componentHarness.getNoIntegrationBanner();
      expect(banner).toBeTruthy();
    });

    it('should be hidden when integration are present', async (): Promise<void> => {
      expectIntegrationGetRequest([fakeIntegration()]);
      const banner: TestElement = await componentHarness.getNoIntegrationBanner();
      expect(banner).toBeFalsy();
    });
  });

  function expectIntegrationGetRequest(
    fakeIntegrations: Integration[] = [fakeIntegration(), fakeIntegration(), fakeIntegration()],
    page: number = 1,
    size: number = 10,
  ): void {
    const fakeIntegrationResponse: IntegrationResponse = {
      data: fakeIntegrations,
      pagination: {},
    };

    const req: TestRequest = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/integrations?page=${page}&perPage=${size}`,
    );
    req.flush(fakeIntegrationResponse);
    expect(req.request.method).toEqual('GET');
  }

  function expectConsoleSettingsGetRequest(consoleSettingsResponse: ConsoleSettings) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
    req.flush(consoleSettingsResponse);
    expect(req.request.method).toEqual('GET');
  }
});
