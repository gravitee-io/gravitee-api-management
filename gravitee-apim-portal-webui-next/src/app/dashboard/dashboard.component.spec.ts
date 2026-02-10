/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, provideRouter, Router, Routes } from '@angular/router';

import { routes } from '../app.routes';
import { DashboardComponent } from './dashboard.component';
import { DashboardComponentHarness } from './dashboard.component.harness';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let harness: DashboardComponentHarness;
  let router: Router;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    const dashboardRoute = (routes as Routes).find(route => route.path === 'dashboard');

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule, AppTestingModule],
      providers: [
        provideRouter([
          {
            path: '',
            component: DashboardComponent,
            children: dashboardRoute?.children ?? [],
          },
        ]),
        {
          provide: ActivatedRoute,
          useValue: {
            routeConfig: {
              children: dashboardRoute?.children ?? [],
            },
            snapshot: {
              url: [],
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DashboardComponentHarness);
    router = TestBed.inject(Router);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create', async () => {
    expect(await harness.host()).toBeTruthy();
  });

  it('should display Subscriptions sidenav and breadcrumb', async () => {
    await router.navigate(['subscriptions']);
    fixture.detectChanges();

    const applicationsReq = httpTestingController.expectOne(req => req.url.includes('/applications'));
    applicationsReq.flush({ data: [] });

    const subscriptionsReq = httpTestingController.expectOne(req => req.url.includes('/subscriptions'));
    subscriptionsReq.flush({ data: [], links: { self: '' }, metadata: {} });

    const sidenav = await harness.getSidenav();
    expect(await sidenav?.getText()).toContain('Subscriptions');

    const breadcrumbs = await harness.getBreadcrumbs();
    expect(await breadcrumbs?.getText()).toContain('Subscriptions');
  });
});
