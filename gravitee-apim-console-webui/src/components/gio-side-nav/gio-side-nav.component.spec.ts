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
import { of, Subject } from 'rxjs';

import { GioSideNavComponent, PORTAL_SETTINGS_PERMISSIONS, SIDE_NAV_GROUP_ID } from './gio-side-nav.component';
import { GioSideNavHarness } from './gio-side-nav.component.harness';
import { GioSideNavModule } from './gio-side-nav.module';

import { License } from '../../entities/license/License';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { Constants } from '../../entities/Constants';
import { EnvironmentSettingsService } from '../../services-ngx/environment-settings.service';

const PORTAL_SETTINGS_PERMISSIONS_SET = new Set(PORTAL_SETTINGS_PERMISSIONS);

describe('GioSideNavComponent', () => {
  let fixture: ComponentFixture<GioSideNavComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  const menuSearchService = new GioMenuSearchService();

  const expirationDateInOneYear = new Date();
  expirationDateInOneYear.setFullYear(expirationDateInOneYear.getFullYear() + 1);

  const init = async (
    licenseNotificationEnabled = true,
    hasLicenseMgmtPermission = true,
    scoringEnabled = true,
    hasApiProductPermission = true,
    hasPortalSettingsPermission = true,
    portalNextEnabled = true,
  ) => {
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
              const isMatchingApiProductPermission = permissions.length === 1 && permissions[0] === 'environment-api_product-r';
              if (isMatchingApiProductPermission && !hasApiProductPermission) {
                return false;
              }
              const isMatchingPortalSettingsPermissions =
                permissions.length === PORTAL_SETTINGS_PERMISSIONS_SET.size &&
                permissions.every(p => PORTAL_SETTINGS_PERMISSIONS_SET.has(p));
              if (isMatchingPortalSettingsPermissions && !hasPortalSettingsPermission) {
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
        {
          provide: EnvironmentSettingsService,
          useValue: {
            get: () => {
              return of({ apiScore: { enabled: scoringEnabled }, portalNext: { access: { enabled: portalNextEnabled } } });
            },
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
          .then(_ => fail('Should not be able to find title'))
          .catch(err => expect(err).toBeDefined());
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
      const searchItems: { name: string }[] = addSearchItemByGroupIds.mock.calls[0][0];
      const searchItemNames = searchItems.map(i => i.name);

      // flat items with real routerLinks must appear
      expect(searchItemNames).toEqual(
        expect.arrayContaining(['Dashboard', 'APIs', 'API Products', 'Applications', 'Gateways', 'Audit', 'Settings']),
      );
      // group-only items (no routerLink of their own) must NOT appear in search
      expect(searchItemNames).not.toContain('Kafka');
      expect(searchItemNames).not.toContain('Observability');
      expect(searchItemNames).not.toContain('Analytics');
    });
  });

  describe('settings check', () => {
    it('should hide scoring elements when feature is disabled', async () => {
      await init(true, true, false);
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      expect(fixture.componentInstance.mainMenuItems.find(item => item.displayName === 'API Score')).toBeUndefined();
    });

    it('should show scoring elements when feature is enabled', async () => {
      await init(true, true, true);
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      expect(fixture.componentInstance.mainMenuItems.find(item => item.displayName === 'API Score')).toEqual({
        category: 'API Score',
        displayName: 'API Score',
        icon: 'gio:shield-check',
        permissions: ['environment-integration-r'],
        routerLink: './api-score',
      });
    });

    it('should show API Products menu item with license options', async () => {
      await init();
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      const apiProductsItem = fixture.componentInstance.mainMenuItems.find(item => item.displayName === 'API Products');
      expect(apiProductsItem).toBeDefined();
      expect(apiProductsItem?.routerLink).toBe('./api-products');
      expect(apiProductsItem?.licenseOptions).toEqual({
        feature: 'apim-api-products',
        context: 'environment',
      });
      expect(apiProductsItem?.iconRight$).toBeDefined();
    });

    it('should hide API Products menu item when user lacks environment-api_product-r permission', async () => {
      await init(true, true, true, false);
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      const apiProductsItem = fixture.componentInstance.mainMenuItems.find(item => item.displayName === 'API Products');
      expect(apiProductsItem).toBeUndefined();
    });

    it('should show API Products menu item when user has environment-api_product-r permission', async () => {
      await init(true, true, true, true);
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      const apiProductsItem = fixture.componentInstance.mainMenuItems.find(item => item.displayName === 'API Products');
      expect(apiProductsItem).toBeDefined();
    });

    it('should show Portal Settings menu item when user has required portal permissions', async () => {
      await init(true, true, false, true, true);
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      const portalSettingsItem = fixture.componentInstance.mainMenuItems.find(item => item.displayName === 'Portal Settings');
      expect(portalSettingsItem).toBeDefined();
      expect(portalSettingsItem?.routerLink).toBe('./_portal');
      expect(portalSettingsItem?.icon).toBe('gio:monitor');
      expect(portalSettingsItem?.externalLink).toBe(true);
    });

    it('should hide Portal Settings menu item when user lacks all portal permissions', async () => {
      await init(true, true, false, true, false);
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      const portalSettingsItem = fixture.componentInstance.mainMenuItems.find(item => item.displayName === 'Portal Settings');
      expect(portalSettingsItem).toBeUndefined();
    });

    it('should hide Portal Settings menu item when portal next is disabled', async () => {
      await init(true, true, true, true, true, false);
      expectLicense({ tier: '', features: [], packs: [], expiresAt: new Date() });

      const portalSettingsItem = fixture.componentInstance.mainMenuItems.find(item => item.displayName === 'Portal Settings');
      expect(portalSettingsItem).toBeUndefined();
    });
  });

  describe('navigation race condition regression (APIM-14285)', () => {
    it('should set currentEnv before environmentSettingsService.get resolves so menu paths use correct env hrid', async () => {
      const envSettingsSubject = new Subject<{ apiScore: { enabled: boolean } }>();
      const envHrid = 'my-env';

      await TestBed.configureTestingModule({
        declarations: [GioSideNavComponent],
        imports: [NoopAnimationsModule, GioTestingModule, GioSideNavModule, MatIconTestingModule],
        providers: [
          {
            provide: GioPermissionService,
            useValue: { hasAnyMatching: () => true },
          },
          {
            provide: Constants,
            useFactory: () => {
              const constants = CONSTANTS_TESTING;
              constants.org.settings = {
                ...constants.org.settings,
                licenseExpirationNotification: { enabled: true },
              };
              constants.org.currentEnv = { id: 'DEFAULT', name: 'default', hrids: [envHrid], organizationId: 'org' } as any;
              constants.org.environments = [{ id: 'DEFAULT', name: 'default', hrids: [envHrid], organizationId: 'org' }];
              return constants;
            },
          },
          {
            provide: EnvironmentSettingsService,
            useValue: { get: () => envSettingsSubject.asObservable() },
          },
          { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION_TESTING },
          { provide: ActivatedRoute, useValue: { params: of({ envHrid }) } },
          { provide: GioMenuSearchService, useValue: menuSearchService },
        ],
      })
        .overrideProvider(InteractivityChecker, {
          useValue: { isFocusable: () => true, isTabbable: () => true },
        })
        .compileComponents();

      const fix = TestBed.createComponent(GioSideNavComponent);
      const ctrl = TestBed.inject(HttpTestingController);

      fix.detectChanges();
      ctrl.expectOne(`${LICENSE_CONFIGURATION_TESTING.resourceURL}`).flush({ tier: '', features: [], packs: [], expiresAt: new Date() });

      // Settings haven't resolved yet — menu built without envSettings
      const observabilityBefore = fix.componentInstance.mainMenuItems.find(i => i.displayName === 'Observability');
      expect(observabilityBefore?.routerBasePath).toContain(envHrid);

      // Now resolve env settings
      envSettingsSubject.next({ apiScore: { enabled: true } });
      fix.detectChanges();

      // After settings resolve, menu is rebuilt — paths must still use correct env hrid
      const observability = fix.componentInstance.mainMenuItems.find(i => i.displayName === 'Observability');
      const analytics = fix.componentInstance.mainMenuItems.find(i => i.displayName === 'Analytics');
      const kafka = fix.componentInstance.mainMenuItems.find(i => i.displayName === 'Kafka');

      expect(observability?.routerBasePath).toBe(`/${envHrid}/observability`);
      expect(analytics?.routerBasePath).toBe(`/${envHrid}/analytics`);
      expect(kafka?.routerBasePath).toBe(`/${envHrid}/clusters`);

      ctrl.verify();
    });
  });

  describe('target attribute on menu item anchors', () => {
    let sideNavHarness: GioSideNavHarness;

    beforeEach(async () => {
      await init();
      // Use a non-expiring license to prevent GioLicenseExpirationNotificationComponent from
      // rendering its own <a target="_blank"> "Contact Gravitee" link (shown when daysRemaining <= 30).
      expectLicense({ tier: '', features: [], packs: [], expiresAt: expirationDateInOneYear });
      sideNavHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, GioSideNavHarness);
      fixture.detectChanges();
    });

    it('should not add target or rel attribute to anchor elements when menu items have no target', async () => {
      fixture.componentInstance.mainMenuItems = [{ icon: 'gio:home', routerLink: './test', displayName: 'Test', category: 'test' }];
      fixture.componentInstance.footerMenuItems = [];
      fixture.detectChanges();

      expect(await sideNavHarness.countAnchorsWithAnyTarget()).toBe(0);
      expect(await sideNavHarness.countAnchorsWithAnyRel()).toBe(0);
    });

    it('should set target="_blank" and rel="noopener noreferrer" on anchor when main menu item has target set', async () => {
      fixture.componentInstance.mainMenuItems = [
        { icon: 'gio:home', routerLink: './test', displayName: 'Test', category: 'test', target: '_blank' },
      ];
      fixture.detectChanges();

      const anchors = await sideNavHarness.getAnchorsWithTarget('_blank');
      expect(anchors.length).toBe(1);
      expect(await anchors[0].getAttribute('rel')).toBe('noopener noreferrer');
    });

    it('should not set target or rel on anchor when main menu item target is absent', async () => {
      fixture.componentInstance.mainMenuItems = [{ icon: 'gio:home', routerLink: './test', displayName: 'Test', category: 'test' }];
      fixture.detectChanges();

      expect(await sideNavHarness.countAnchorsWithAnyTarget()).toBe(0);
      expect(await sideNavHarness.countAnchorsWithAnyRel()).toBe(0);
    });

    it('should set target="_blank" and rel="noopener noreferrer" on anchor when submenu child item has target set', async () => {
      fixture.componentInstance.mainMenuItems = [
        {
          displayName: 'Group',
          category: 'test',
          routerBasePath: '/test/group',
          items: [{ displayName: 'Child', routerLink: './child', category: 'test', target: '_blank' }],
        },
      ];
      fixture.detectChanges();

      const anchors = await sideNavHarness.getAnchorsWithTarget('_blank');
      expect(anchors.length).toBe(1);
      expect(await anchors[0].getAttribute('rel')).toBe('noopener noreferrer');
    });

    it('should set target="_blank" and rel="noopener noreferrer" on anchor when footer menu item has target set', async () => {
      fixture.componentInstance.mainMenuItems = [];
      fixture.componentInstance.footerMenuItems = [
        { icon: 'gio:building', routerLink: './test-footer', displayName: 'Test Footer', category: 'test', target: '_blank' },
      ];
      fixture.detectChanges();

      const anchors = await sideNavHarness.getAnchorsWithTarget('_blank');
      expect(anchors.length).toBe(1);
      expect(await anchors[0].getAttribute('rel')).toBe('noopener noreferrer');
    });

    it('should not set rel on footer anchor when target is absent', async () => {
      fixture.componentInstance.mainMenuItems = [];
      fixture.componentInstance.footerMenuItems = [
        { icon: 'gio:building', routerLink: './test-footer', displayName: 'Test Footer', category: 'test' },
      ];
      fixture.detectChanges();

      expect(await sideNavHarness.countAnchorsWithAnyRel()).toBe(0);
    });
  });

  function expectLicense(license: License) {
    httpTestingController.expectOne(`${LICENSE_CONFIGURATION_TESTING.resourceURL}`).flush(license);
  }
});
