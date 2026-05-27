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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { Dashboard } from '@gravitee/gravitee-dashboard';

import AnalyticsDetailsComponent from './analytics-details.component';
import { AnalyticsDetailsHarness } from './analytics-details.harness';
import { fakeDashboard } from '../../../../entities/analytics-dashboard/analytics-dashboard.fixtures';
import { BreadcrumbService } from '../../../../services/breadcrumb.service';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';
import { analyticsListBreadcrumb } from '../analytics-breadcrumbs';

describe('AnalyticsDetailsComponent', () => {
  let fixture: ComponentFixture<AnalyticsDetailsComponent>;
  let httpTestingController: HttpTestingController;
  let harness: AnalyticsDetailsHarness;

  const DASHBOARD_ID = 'dashboard-1';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsDetailsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsDetailsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('dashboardId', DASHBOARD_ID);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  async function setup(response: Dashboard = fakeDashboard()) {
    fixture.detectChanges();
    httpTestingController.expectOne(req => req.url === `${TESTING_BASE_URL}/analytics/dashboards/${DASHBOARD_ID}`).flush(response);
    await fixture.whenStable();
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AnalyticsDetailsHarness);
  }

  it('should_display_gd_dashboard_component_after_loading', async () => {
    await setup();
    expect(await harness.isDashboardDisplayed()).toBe(true);
  });

  it('should_show_loader_while_loading', async () => {
    fixture.detectChanges();
    // Read the loader from the DOM, not the harness: harnessForFixture() stabilizes and would
    // deadlock on the still-pending rxResource request (flush only happens below).
    expect(fixture.nativeElement.querySelector('app-loader')).not.toBeNull();

    httpTestingController.expectOne(req => req.url === `${TESTING_BASE_URL}/analytics/dashboards/${DASHBOARD_ID}`).flush(fakeDashboard());
    await fixture.whenStable();
    fixture.detectChanges();

    // Request settled → the harness no longer deadlocks.
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AnalyticsDetailsHarness);
    expect(await harness.getLoader()).toBeNull();
  });

  it('should_show_error_when_dashboard_load_fails', async () => {
    fixture.detectChanges();
    httpTestingController
      .expectOne(req => req.url === `${TESTING_BASE_URL}/analytics/dashboards/${DASHBOARD_ID}`)
      .flush('Not Found', { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AnalyticsDetailsHarness);
    expect(await harness.getErrorMessage()).toContain('error occurred while loading the dashboard');
    expect(await harness.isDashboardDisplayed()).toBe(false);
  });

  it('should_set_breadcrumbs_with_dashboard_name', async () => {
    const dashboard = fakeDashboard({ name: 'My Dashboard' });
    await setup(dashboard);
    const breadcrumbService = TestBed.inject(BreadcrumbService);
    expect(breadcrumbService.breadcrumbs()).toEqual([
      analyticsListBreadcrumb(true),
      { id: `analytics-${DASHBOARD_ID}`, label: 'My Dashboard' },
    ]);
  });

  it('should_use_dashboard_id_as_breadcrumb_label_when_name_is_empty', async () => {
    const dashboard = fakeDashboard({ name: '' });
    await setup(dashboard);
    const breadcrumbService = TestBed.inject(BreadcrumbService);
    const breadcrumbs = breadcrumbService.breadcrumbs();
    expect(breadcrumbs[1].label).toBe(DASHBOARD_ID);
  });
});
