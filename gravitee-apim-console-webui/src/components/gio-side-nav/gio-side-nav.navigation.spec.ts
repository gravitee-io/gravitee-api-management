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
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Router, RouterModule, Routes } from '@angular/router';
import { GioMenuSearchService, LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { of } from 'rxjs';

import { GioSideNavModule } from './gio-side-nav.module';
import { GioSideNavHarness } from './gio-side-nav.component.harness';

import { Constants } from '../../entities/Constants';
import { Environment } from '../../entities/environment/environment';
import { EnvironmentGuard } from '../../management/environment.guard';
import { SettingsNavigationService } from '../../management/settings/settings-navigation/settings-navigation.service';
import { EnvironmentSettingsService } from '../../services-ngx/environment-settings.service';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING } from '../../shared/testing';
import { ApimFeature, getFeatureInfoData } from '../../shared/components/gio-license/gio-license-data';

@Component({ template: '', standalone: false })
class PageComponent {}

// Stands in for ManagementComponent: instantiated by the router only once EnvironmentGuard has resolved.
@Component({ template: '<gio-side-nav></gio-side-nav><router-outlet></router-outlet>', standalone: false })
class ManagementShellComponent {}

// Stands in for the application root: only the root outlet, so nothing renders before the first navigation.
@Component({ template: '<router-outlet></router-outlet>', standalone: false })
class RootComponent {}

// Mirrors the license configuration built at bootstrap in index.ts, so the gioLicense directive can
// resolve the APIM features the side nav declares.
const LICENSE_CONFIGURATION = { ...LICENSE_CONFIGURATION_TESTING, featureInfoData: getFeatureInfoData(undefined) };

// Mirrors the real route tree: management-routing.module.ts plus the default redirects declared by
// observability.module.ts and env-analytics-legacy.module.ts.
const routes: Routes = [
  {
    path: ':envHrid',
    component: ManagementShellComponent,
    canActivate: [EnvironmentGuard.initEnvConfigAndLoadPermissions],
    children: [
      { path: 'home', component: PageComponent },
      {
        path: 'observability',
        children: [
          { path: 'overview', component: PageComponent },
          { path: '', pathMatch: 'full', redirectTo: 'overview' },
        ],
      },
      {
        path: 'analytics',
        children: [
          { path: 'dashboard', component: PageComponent },
          { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
        ],
      },
      { path: '', pathMatch: 'full', redirectTo: 'home' },
    ],
  },
];

/**
 * Drives the real navigation chain a user triggers by clicking a sidebar group:
 * GioSideNavComponent builds routerBasePath -> GioMenuItemsComponent#onHeaderClick calls
 * router.navigate -> EnvironmentGuard resolves the :envHrid segment.
 *
 * Only the HTTP boundary is stubbed; the environments payload is the one a Management API returns
 * for an environment that predates hrids.
 */
describe('GioSideNavComponent navigation (APIM-14748)', () => {
  let fixture: ComponentFixture<RootComponent>;
  let router: Router;
  let httpTestingController: HttpTestingController;

  const MAX_SETTLE_ROUNDS = 20;

  const DEFAULT_ENV_WITHOUT_HRIDS: Environment = {
    id: 'DEFAULT',
    name: 'Default environment',
    organizationId: 'DEFAULT',
  };

  const flushPendingEnvironmentCalls = (environment: Environment): number => {
    const requests = httpTestingController.match(req => req.url.endsWith('/environments'));
    requests.forEach(request => request.flush([environment]));
    return requests.length;
  };

  /**
   * An enterprise license, so the license directive lets every group through to the router instead of
   * intercepting the click to offer an upgrade.
   */
  const flushLicense = (): number => {
    const requests = httpTestingController.match(req => req.url === LICENSE_CONFIGURATION_TESTING.resourceURL);
    requests.forEach(request =>
      request.flush({
        tier: 'universe',
        features: [ApimFeature.APIM_CLUSTER, ApimFeature.APIM_AUDIT_TRAIL, ApimFeature.APIM_API_PRODUCTS, ApimFeature.ALERT_ENGINE],
        packs: [],
        expiresAt: new Date(),
      }),
    );
    return requests.length;
  };

  /**
   * Answers requests until the router is idle and nothing is left to answer. Each guard hop fetches
   * the environment list again, so the number of rounds is not knowable here; looping on quiescence
   * rather than a fixed count keeps assertions off half-finished navigations, and the throw makes a
   * chain that grows an extra hop fail with its own message instead of a puzzling URL mismatch.
   */
  const settleNavigation = (environment: Environment) => {
    for (let round = 0; round < MAX_SETTLE_ROUNDS; round++) {
      tick();
      fixture.detectChanges();

      const answered = flushPendingEnvironmentCalls(environment) + flushLicense();
      if (answered === 0 && !router.getCurrentNavigation()) {
        return;
      }
    }
    throw new Error(`Navigation did not settle within ${MAX_SETTLE_ROUNDS} rounds (router.url=${router.url})`);
  };

  const setup = (environment: Environment) => {
    TestBed.configureTestingModule({
      declarations: [RootComponent, ManagementShellComponent, PageComponent],
      imports: [NoopAnimationsModule, MatIconTestingModule, GioSideNavModule, RouterModule.forRoot(routes)],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Constants, useValue: { ...CONSTANTS_TESTING, org: { ...CONSTANTS_TESTING.org, environments: [environment] } } },
        { provide: GioPermissionService, useValue: { hasAnyMatching: () => true, loadEnvironmentPermissions: () => of(undefined) } },
        {
          provide: EnvironmentSettingsService,
          useValue: {
            load: () => of(undefined),
            get: () => of({ apiScore: { enabled: true }, portalNext: { access: { enabled: false } } }),
          },
        },
        { provide: SettingsNavigationService, useValue: { getSettingsNavigationSearchItems: () => [] } },
        { provide: GioMenuSearchService, useValue: new GioMenuSearchService() },
        { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION },
      ],
    }).overrideProvider(InteractivityChecker, { useValue: { isFocusable: () => true, isTabbable: () => true } });

    router = TestBed.inject(Router);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RootComponent);
    fixture.detectChanges();
  };

  /** The environment segment plus the group, dropping whatever the group's own module redirects to. */
  const groupPathOf = (url: string): string => url.split('?')[0].split('/').slice(0, 3).join('/');

  const landOnUrl = (url: string, environment: Environment) => {
    let navigationError: unknown;
    router.navigateByUrl(url).catch(error => (navigationError = error));
    settleNavigation(environment);

    // A guard blowing up on the way in would otherwise resurface as a puzzling assertion on the
    // post-click URL, several lines later.
    if (navigationError) {
      throw navigationError;
    }
    expect(router.url).toBe(url);
  };

  const clickSidebarGroup = (title: string, environment: Environment) => {
    // The harness is async while the test drives a virtual clock, so the promise chain is started
    // and then flushed by tick() rather than awaited.
    let clicked = false;
    TestbedHarnessEnvironment.harnessForFixture(fixture, GioSideNavHarness)
      .then(sideNav => sideNav.clickGroup(title))
      .then(() => (clicked = true));
    tick();
    expect(clicked).toBe(true);

    settleNavigation(environment);
  };

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
  });

  it('should navigate to Observability when the environment has no hrids', fakeAsync(() => {
    setup(DEFAULT_ENV_WITHOUT_HRIDS);

    landOnUrl('/DEFAULT/home', DEFAULT_ENV_WITHOUT_HRIDS);
    clickSidebarGroup('Observability', DEFAULT_ENV_WITHOUT_HRIDS);

    expect(groupPathOf(router.url)).toBe('/DEFAULT/observability');
  }));

  it('should navigate to Analytics when the environment has no hrids', fakeAsync(() => {
    setup(DEFAULT_ENV_WITHOUT_HRIDS);

    landOnUrl('/DEFAULT/home', DEFAULT_ENV_WITHOUT_HRIDS);

    clickSidebarGroup('Analytics', DEFAULT_ENV_WITHOUT_HRIDS);

    expect(groupPathOf(router.url)).toBe('/DEFAULT/analytics');
  }));

  it('should keep using the hrid when the environment has one', fakeAsync(() => {
    const environmentWithHrid: Environment = { ...DEFAULT_ENV_WITHOUT_HRIDS, hrids: ['default'] };
    setup(environmentWithHrid);

    landOnUrl('/default/home', environmentWithHrid);

    clickSidebarGroup('Observability', environmentWithHrid);

    expect(groupPathOf(router.url)).toBe('/default/observability');
  }));
});
