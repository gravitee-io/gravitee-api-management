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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { BehaviorSubject, of } from 'rxjs';

import { ClassicPortalOnlyBannerComponent } from './classic-portal-only-banner.component';
import { ClassicPortalOnlyBannerHarness } from './classic-portal-only-banner.component.harness';

import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { GioPermissionService } from '../gio-permission/gio-permission.service';
import { PortalNavigationItemService } from '../../../services-ngx/portal-navigation-item.service';
import { PortalNavigationItem } from '../../../entities/management-api-v2';
import { fakePortalNavigationApi } from '../../../entities/management-api-v2/portalNavigationItem/portalNavigationItem.fixture';

@Component({
  template: ` <classic-portal-only-banner [title]="title" [body]="body" [actionLabel]="actionLabel" /> `,
  standalone: true,
  imports: [ClassicPortalOnlyBannerComponent],
})
class TestHostComponent {
  title = 'Classic Developer Portal Only';
  body = '';
  actionLabel = 'Next Gen Portal Settings';
}

function buildTestBed(
  portalNextEnabled$: BehaviorSubject<boolean>,
  hasSettingsPermission = true,
  envHrid: string | null = 'test-env',
  apiId: string | null = null,
  navigationItems: PortalNavigationItem[] = [],
) {
  return TestBed.configureTestingModule({
    imports: [TestHostComponent, MatIconTestingModule],
    providers: [
      provideRouter([]),
      provideNoopAnimations(),
      {
        provide: EnvironmentSettingsService,
        useValue: { isPortalNextEnabled: () => portalNextEnabled$.asObservable() },
      },
      {
        provide: GioPermissionService,
        useValue: { hasAnyMatching: () => hasSettingsPermission },
      },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { params: { ...(envHrid != null ? { envHrid } : {}), ...(apiId != null ? { apiId } : {}) } } },
      },
      {
        provide: PortalNavigationItemService,
        useValue: { getNavigationItems: () => of({ items: navigationItems }) },
      },
    ],
  }).compileComponents();
}

describe('ClassicPortalOnlyBannerComponent — portal-next gating', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let loader: HarnessLoader;
  let harness: ClassicPortalOnlyBannerHarness;
  let portalNextEnabled$: BehaviorSubject<boolean>;

  beforeEach(async () => {
    portalNextEnabled$ = new BehaviorSubject<boolean>(false);
    await buildTestBed(portalNextEnabled$);

    fixture = TestBed.createComponent(TestHostComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(ClassicPortalOnlyBannerHarness);
    fixture.detectChanges();
  });

  it('should show the banner when portalNext.access.enabled is true', async () => {
    portalNextEnabled$.next(true);
    fixture.detectChanges();
    expect(await harness.isBannerContentVisible()).toBe(true);
  });

  it('should hide the banner when portalNext.access.enabled is false', async () => {
    portalNextEnabled$.next(false);
    fixture.detectChanges();
    expect(await harness.isBannerContentVisible()).toBe(false);
  });

  it('should hide the banner before the settings have been loaded (initial false value)', async () => {
    expect(await harness.isBannerContentVisible()).toBe(false);
  });

  it('should update visibility reactively when the setting changes', async () => {
    portalNextEnabled$.next(true);
    fixture.detectChanges();
    expect(await harness.isBannerContentVisible()).toBe(true);

    portalNextEnabled$.next(false);
    fixture.detectChanges();
    expect(await harness.isBannerContentVisible()).toBe(false);
  });
});

describe('ClassicPortalOnlyBannerComponent — inputs', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let loader: HarnessLoader;
  let harness: ClassicPortalOnlyBannerHarness;

  beforeEach(async () => {
    const portalNextEnabled$ = new BehaviorSubject<boolean>(true);
    await buildTestBed(portalNextEnabled$);

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(ClassicPortalOnlyBannerHarness);
    fixture.detectChanges();
  });

  it('should render the default title', async () => {
    expect(await harness.getTitleText()).toBe('Classic Developer Portal Only');
  });

  it('should render a custom title', async () => {
    host.title = 'Custom Title';
    fixture.detectChanges();
    expect(await harness.getTitleText()).toBe('Custom Title');
  });

  it('should not render the body when body input is empty', async () => {
    expect(await harness.getBodyText()).toBeNull();
  });

  it('should render the body when body input is provided', async () => {
    host.body = 'This documentation is used by the Classic Developer Portal.';
    fixture.detectChanges();
    expect(await harness.getBodyText()).toBe('This documentation is used by the Classic Developer Portal.');
  });

  it('should show the settings action with default label', async () => {
    expect(await harness.isSettingsActionVisible()).toBe(true);
    expect(await harness.getSettingsActionText()).toBe('Next Gen Portal Settings');
  });

  it('should show the settings action with a custom label', async () => {
    host.actionLabel = 'Go to Portal Settings';
    fixture.detectChanges();
    expect(await harness.getSettingsActionText()).toBe('Go to Portal Settings');
  });

  it('should set rel="noopener noreferrer" on the settings action link', async () => {
    expect(await harness.getSettingsActionRelAttribute()).toBe('noopener noreferrer');
  });
});

describe('ClassicPortalOnlyBannerComponent — settings action permission and route gating', () => {
  const portalNextEnabled$ = new BehaviorSubject<boolean>(true);

  it('should show the settings action when user has environment-settings-r and envHrid is in route', async () => {
    await buildTestBed(portalNextEnabled$, true, 'my-env');
    const fixture = TestBed.createComponent(TestHostComponent);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    const harness = await loader.getHarness(ClassicPortalOnlyBannerHarness);
    expect(await harness.isSettingsActionVisible()).toBe(true);
  });

  it('should hide the settings action when user lacks environment-settings permissions', async () => {
    await buildTestBed(portalNextEnabled$, false, 'my-env');
    const fixture = TestBed.createComponent(TestHostComponent);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    const harness = await loader.getHarness(ClassicPortalOnlyBannerHarness);
    expect(await harness.isSettingsActionVisible()).toBe(false);
  });

  it('should hide the settings action when route has no envHrid', async () => {
    await buildTestBed(portalNextEnabled$, true, null);
    const fixture = TestBed.createComponent(TestHostComponent);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    const harness = await loader.getHarness(ClassicPortalOnlyBannerHarness);
    expect(await harness.isSettingsActionVisible()).toBe(false);
  });
});

describe('ClassicPortalOnlyBannerComponent — navId query param resolution', () => {
  const portalNextEnabled$ = new BehaviorSubject<boolean>(true);

  it('should include navId query param in the settings link when apiId matches a navigation item', async () => {
    const navItem = fakePortalNavigationApi({ id: 'nav-id-1', apiId: 'my-api-id' });
    await buildTestBed(portalNextEnabled$, true, 'test-env', 'my-api-id', [navItem]);
    const fixture = TestBed.createComponent(TestHostComponent);
    const harness = await TestbedHarnessEnvironment.loader(fixture).getHarness(ClassicPortalOnlyBannerHarness);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.getSettingsActionHref()).toContain('navId=nav-id-1');
  });

  it('should not include navId query param when no navigation item matches the apiId', async () => {
    const navItem = fakePortalNavigationApi({ id: 'nav-id-1', apiId: 'other-api-id' });
    await buildTestBed(portalNextEnabled$, true, 'test-env', 'my-api-id', [navItem]);
    const fixture = TestBed.createComponent(TestHostComponent);
    const harness = await TestbedHarnessEnvironment.loader(fixture).getHarness(ClassicPortalOnlyBannerHarness);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.getSettingsActionHref()).not.toContain('navId=');
  });

  it('should not include navId query param when no apiId is in the route', async () => {
    await buildTestBed(portalNextEnabled$, true, 'test-env', null, []);
    const fixture = TestBed.createComponent(TestHostComponent);
    const harness = await TestbedHarnessEnvironment.loader(fixture).getHarness(ClassicPortalOnlyBannerHarness);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.getSettingsActionHref()).not.toContain('navId=');
  });
});
