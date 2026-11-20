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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { PortalNavigationComponent } from './portal-navigation.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { Constants, DefaultPortal } from '../../entities/Constants';
import { fakePortalSettings } from '../../entities/portal/portalSettings.fixture';
import { GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';

describe('PortalNavigationComponent', () => {
  let fixture: ComponentFixture<PortalNavigationComponent>;
  let httpTestingController: HttpTestingController;

  const init = async (defaultPortal: DefaultPortal) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalNavigationComponent],
      providers: [
        { provide: Constants, useValue: { ...CONSTANTS_TESTING, defaultPortal } },
        { provide: GioTestingPermissionProvider, useValue: ['environment-settings-r'] },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PortalNavigationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    const portalSettings = fakePortalSettings();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`)
      .flush({ ...portalSettings, portal: { ...portalSettings.portal, url: 'https://portal.test' } });
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const openWebsiteHref = (): string | null =>
    (fixture.nativeElement as HTMLElement).querySelector('gio-top-bar-link a')?.getAttribute('href') ?? null;

  const portalRedirectUrl = `${CONSTANTS_TESTING.env.baseURL}/portal/redirect`;

  describe('Open Website link', () => {
    it('should open the portal root when the New Developer Portal is the default portal', async () => {
      await init('next');

      expect(openWebsiteHref()).toEqual(portalRedirectUrl);
    });

    it('should open the New Developer Portal at /next when the Classic portal is the default portal', async () => {
      await init('classic');

      expect(openWebsiteHref()).toEqual(`${portalRedirectUrl}?version=next`);
    });

    it('should open the New Developer Portal at /next when no default portal is configured', async () => {
      await init(null);

      expect(openWebsiteHref()).toEqual(`${portalRedirectUrl}?version=next`);
    });
  });
});
