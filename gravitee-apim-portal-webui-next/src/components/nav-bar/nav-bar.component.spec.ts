/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentRef, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { of } from 'rxjs';

import { NavBarComponent } from './nav-bar.component';
import { PortalPage } from '../../entities/portal/portal-page';
import { PortalNavigationItem, PortalNavigationLink } from '../../entities/portal-navigation/portal-navigation-item';
import { fakeUser } from '../../entities/user/user.fixtures';
import { PortalNavigationItemsService } from '../../services/portal-navigation-items.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';
import { DivHarness } from '../../testing/div.harness';

describe('NavBarComponent', () => {
  let fixture: ComponentFixture<NavBarComponent>;
  let harnessLoader: HarnessLoader;
  let componentRef: ComponentRef<NavBarComponent>;
  let httpTestingController: HttpTestingController;
  const topBarNavigationItems: PortalNavigationItem[] = [
    {
      id: 'link-id-1',
      organizationId: 'DEFAULT',
      environmentId: 'DEFAULT',
      title: 'link-name-1',
      type: 'LINK',
      area: 'TOP_NAVBAR',
      order: 1,
      url: 'link-target-1',
    } as PortalNavigationLink,
    {
      id: 'link-id-2',
      organizationId: 'DEFAULT',
      environmentId: 'DEFAULT',
      title: 'link-name-2',
      type: 'LINK',
      area: 'TOP_NAVBAR',
      order: 2,
      url: 'link-target-2',
    } as PortalNavigationLink,
  ];

  const init = async (isMobile: boolean = false) => {
    const mockBreakpointObserver = {
      observe: () => of({ matches: isMobile, breakpoints: { [Breakpoints.XSmall]: isMobile } }),
    };

    const portalNavigationItemsServiceMock: Partial<PortalNavigationItemsService> = {
      loadTopNavBarItems: () => of(undefined),
      topNavbarItems: {
        set: () => {},
      } as unknown as WritableSignal<PortalNavigationItem[]>,
    };

    await TestBed.configureTestingModule({
      imports: [NavBarComponent, AppTestingModule],
      providers: [
        { provide: BreakpointObserver, useValue: mockBreakpointObserver },
        { provide: PortalNavigationItemsService, useValue: portalNavigationItemsServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NavBarComponent);
    componentRef = fixture.componentRef;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  describe('using desktop view', () => {
    beforeEach(async () => {
      await init();
    });

    afterEach(() => {
      httpTestingController.verify();
    });

    it('should show login button if user not connected', async () => {
      let logInButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Sign in' }));
      expect(logInButton).toBeTruthy();
      componentRef.setInput('currentUser', fakeUser());
      logInButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Sign in' }));
      expect(logInButton).toBeFalsy();
    });

    it('should not show links if user is not connected and login is forced', async () => {
      componentRef.setInput('topBarNavigationItems', topBarNavigationItems);
      componentRef.setInput('forceLogin', true);
      const link1Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'link-name-1' }));
      expect(link1Anchor).not.toBeTruthy();
      const link2Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'link-name-2' }));
      expect(link2Anchor).not.toBeTruthy();
    });

    it('should show links if user is connected and login is forced', async () => {
      componentRef.setInput('topBarNavigationItems', topBarNavigationItems);
      componentRef.setInput('forceLogin', true);
      componentRef.setInput('currentUser', fakeUser());
      const link1Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: /link-name-1/i }));
      expect(link1Anchor).toBeTruthy();
      const link2Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: /link-name-2/i }));
      expect(link2Anchor).toBeTruthy();
    });

    it('should show custom links if login is not forced', async () => {
      componentRef.setInput('topBarNavigationItems', topBarNavigationItems);
      const link1Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: /link-name-1/i }));
      expect(link1Anchor).toBeTruthy();
      const link2Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: /link-name-2/i }));
      expect(link2Anchor).toBeTruthy();
    });
  });

  describe('using mobile view', () => {
    beforeEach(async () => {
      await init(true);
    });

    afterEach(() => {
      httpTestingController.verify();
    });

    it('should show login button if user not connected', async () => {
      expectHomePage();
      fixture.detectChanges();

      const menuButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.mobile-menu__button' }));
      await menuButton.click();

      const links: NodeList = fixture.debugElement.nativeElement.querySelectorAll('.mobile-menu__link');
      const linkTexts = Array.from(links).map((el: Node) => el.textContent?.trim());
      expect(linkTexts).toEqual(['Homepage', 'Catalog', 'Sign in']);
    });

    it('should show logout button if user connected', async () => {
      expectHomePage();
      componentRef.setInput('currentUser', fakeUser());
      fixture.detectChanges();

      const menuButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.mobile-menu__button' }));
      await menuButton.click();

      const links: NodeList = fixture.debugElement.nativeElement.querySelectorAll('.mobile-menu__link');
      const linkTexts = Array.from(links).map((el: Node) => el.textContent?.trim());
      expect(linkTexts).toEqual(['Homepage', 'Catalog', 'Applications', 'Subscriptions', 'Log out']);
    });

    it('should not show menu if user is not connected and login is forced', async () => {
      expectHomePage([]);
      componentRef.setInput('topBarNavigationItems', topBarNavigationItems);
      componentRef.setInput('forceLogin', true);
      fixture.detectChanges();

      const menuButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.mobile-menu__button' }));
      expect(menuButton).not.toBeTruthy();
    });

    it('should show links if user is connected and login is forced', async () => {
      expectHomePage([]);
      componentRef.setInput('currentUser', fakeUser());
      componentRef.setInput('topBarNavigationItems', topBarNavigationItems);
      componentRef.setInput('forceLogin', true);
      fixture.detectChanges();

      const menuButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.mobile-menu__button' }));
      await menuButton.click();
      fixture.detectChanges();

      const links: NodeList = fixture.debugElement.nativeElement.querySelectorAll('.mobile-menu__link');
      const linkTexts = Array.from(links).map((el: Node) => el.textContent?.trim());
      expect(linkTexts).toEqual([
        'Catalog',
        'link-name-1 open_in_new(opens in new tab)',
        'link-name-2 open_in_new(opens in new tab)',
        'Applications',
        'Subscriptions',
        'Log out',
      ]);
    });

    it('should show custom links if login is not forced', async () => {
      expectHomePage([]);
      componentRef.setInput('topBarNavigationItems', topBarNavigationItems);
      fixture.detectChanges();

      const menuButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.mobile-menu__button' }));
      await menuButton.click();
      fixture.detectChanges();

      const links: NodeList = fixture.debugElement.nativeElement.querySelectorAll('.mobile-menu__link');
      const linkTexts = Array.from(links).map((el: Node) => el.textContent?.trim());
      expect(linkTexts).toEqual([
        'Catalog',

        'link-name-1 open_in_new(opens in new tab)',
        'link-name-2 open_in_new(opens in new tab)',
        'Sign in',
      ]);
    });

    it('should close menu when clicking outside', async () => {
      componentRef.setInput('currentUser', fakeUser());
      expectHomePage();
      fixture.detectChanges();

      const menuButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.mobile-menu__button' }));
      await menuButton.click();
      fixture.detectChanges();

      expect(await harnessLoader.getHarness(DivHarness.with({ selector: '.mobile-menu__panel' }))).toBeTruthy();

      const clickEvent = new MouseEvent('click', {
        bubbles: true,
        cancelable: true,
        clientX: 0,
        clientY: 0,
      });
      const outsideElement = document.createElement('div');
      Object.defineProperty(clickEvent, 'target', {
        value: outsideElement,
        writable: false,
      });
      document.dispatchEvent(clickEvent);
      fixture.detectChanges();

      expect(await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.mobile-menu__panel' }))).toBeNull();
    });

    it('should handle error when fetching homepage', async () => {
      expectHomePageWithError();
      fixture.detectChanges();

      const menuButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.mobile-menu__button' }));
      await menuButton.click();

      const links: NodeList = fixture.debugElement.nativeElement.querySelectorAll('.mobile-menu__link');
      const linkTexts = Array.from(links).map((el: Node) => el.textContent?.trim());
      expect(linkTexts).toEqual(['Catalog', 'Sign in']);
    });
  });

  function expectHomePage(pages: PortalPage[] = [{ id: '1', name: 'Homepage', type: 'HOMEPAGE' }]) {
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-pages?type=HOMEPAGE`);
    req.flush({ pages });
  }

  function expectHomePageWithError() {
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-pages?type=HOMEPAGE`);
    req.flush('Error', { status: 500, statusText: 'Server Error' });
  }
});
