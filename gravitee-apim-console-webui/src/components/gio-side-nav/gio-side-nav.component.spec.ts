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
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import {
  GioLicenseExpirationNotificationHarness,
  GioMenuSearchService,
  LICENSE_CONFIGURATION_TESTING,
} from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { GioSideNavComponent, SIDE_NAV_GROUP_ID } from './gio-side-nav.component';
import { GioSideNavModule } from './gio-side-nav.module';

import { License } from '../../entities/license/License';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { Constants } from '../../entities/Constants';

describe('GioSideNavComponent', () => {
  let fixture: ComponentFixture<GioSideNavComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  const menuSearchService = new GioMenuSearchService();

  const expirationDateInOneYear = new Date();
  expirationDateInOneYear.setFullYear(expirationDateInOneYear.getFullYear() + 1);

  const init = async (licenseNotificationEnabled = true, hasLicenseMgmtPermission = true, scoringEnabled = true) => {
    await TestBed.configureTestingModule({
      declarations: [GioSideNavComponent],
      imports: [NoopAnimationsModule, GioTestingModule, GioSideNavModule, MatIconTestingModule],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: (permissions: string[]) => {
              const isMatchingLicenseNotificationPermission =
                permissions.length === 1 && permissions[0] === 'organization-license_management-r';
              if (isMatchingLicenseNotificationPermission && !hasLicenseMgmtPermission) {
                return false;
              }
              // Return default true for all the rest of 'hasMatching' permission checks
              return true;
            },
          },
        },
        {
          provide: Constants,
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            constants.org.settings = {
              ...constants.org.settings,
              licenseExpirationNotification: { enabled: licenseNotificationEnabled },
              scoring: { enabled: scoringEnabled },
            };
            constants.org.environments = [
              {
                id: 'DEFAULT',
                name: 'default',
                hrids: [],
                organizationId: 'organizationId',
              },
            ];

            return constants;
          },
        },
        { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION_TESTING },
        { provide: ActivatedRoute, useValue: { params: of({ envId: 'DEFAULT' }) } },
        { provide: GioMenuSearchService, useValue: menuSearchService },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(GioSideNavComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('License expiration notification', () => {
    describe('when configuration is enabled', () => {
      beforeEach(async () => {
        await init();
      });
      it('should load the component with expired license', async () => {
        expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

        const expirationNotification = await loader.getHarness(GioLicenseExpirationNotificationHarness);
        expect(expirationNotification).toBeDefined();

        expect(await expirationNotification.getTitleText()).toEqual('Your license has expired');
      });

      it.each([expirationDateInOneYear, undefined])('should not show notification', async (date: Date) => {
        expectLicense({ tier: '', features: [], packs: [], expiresAt: date });

        const expirationNotification = await loader.getHarness(GioLicenseExpirationNotificationHarness);
        await expirationNotification
          .getTitleText()
          .then((_) => fail('Should not be able to find title'))
          .catch((err) => expect(err).toBeDefined());
      });
    });

    describe('when configuration is disabled', () => {
      beforeEach(async () => {
        await init(false);
      });
      it('should not load the component with expired license', async () => {
        expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

        const expirationNotification = await loader.getHarnessOrNull(GioLicenseExpirationNotificationHarness);
        expect(expirationNotification).toBeNull();
      });
    });

    describe('when permission is not present', () => {
      beforeEach(async () => {
        await init(true, false);
      });
      it('should not load the component with expired license', async () => {
        expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

        const expirationNotification = await loader.getHarnessOrNull(GioLicenseExpirationNotificationHarness);
        expect(expirationNotification).toBeNull();
      });
    });
  });

  describe('Side nav search test', () => {
    it('should remove previous search entries and then add new ones', async () => {
      const removeSearchItemByGroupIds = jest.spyOn(menuSearchService, 'removeMenuSearchItems');
      const addSearchItemByGroupIds = jest.spyOn(menuSearchService, 'addMenuSearchItems');

      await init();
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      expect(removeSearchItemByGroupIds).toHaveBeenCalledTimes(1);
      expect(removeSearchItemByGroupIds).toHaveBeenCalledWith([SIDE_NAV_GROUP_ID]);
      expect(addSearchItemByGroupIds).toHaveBeenCalledTimes(1);
      expect(addSearchItemByGroupIds).toHaveBeenCalledWith(
        expect.arrayContaining([
          expect.objectContaining({ name: 'Dashboard', routerLink: expect.not.stringContaining('./') }),
          expect.objectContaining({ name: 'APIs', routerLink: expect.not.stringContaining('./') }),
          expect.objectContaining({ name: 'Applications', routerLink: expect.not.stringContaining('./') }),
          expect.objectContaining({ name: 'Gateways', routerLink: expect.not.stringContaining('./') }),
          expect.objectContaining({ name: 'Audit', routerLink: expect.not.stringContaining('./') }),
          expect.objectContaining({ name: 'Analytics', routerLink: expect.not.stringContaining('./') }),
          expect.objectContaining({ name: 'Settings', routerLink: expect.not.stringContaining('./') }),
        ]),
      );
    });
  });

  describe('settings check', () => {
    it('should hide scoring elements when feature is disabled', async () => {
      await init(true, true, false);
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      expect(fixture.componentInstance.mainMenuItems.find((item) => item.displayName === 'API Score')).toBeUndefined();
    });

    it('should show scoring elements when feature is enabled', async () => {
      await init(true, true, true);
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      expect(fixture.componentInstance.mainMenuItems.find((item) => item.displayName === 'API Score')).toEqual({
        category: 'API Score',
        displayName: 'API Score',
        icon: 'gio:shield-check',
        permissions: ['environment-integration-r'],
        routerLink: './api-score',
      });
    });
  });

  function expectLicense(license: License) {
    httpTestingController.expectOne(`${LICENSE_CONFIGURATION_TESTING.resourceURL}`).flush(license);
  }
});
