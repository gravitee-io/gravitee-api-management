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
import { Router, provideRouter } from '@angular/router';

import { analyticsListBreadcrumb } from './analytics-breadcrumbs';
import AnalyticsComponent from './analytics.component';
import { AnalyticsComponentHarness } from './analytics.harness';
import { AnalyticsDashboardsResponse } from '../../../entities/analytics-dashboard/analytics-dashboard';
import { fakeDashboard, fakeAnalyticsDashboardsResponse } from '../../../entities/analytics-dashboard/analytics-dashboard.fixtures';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { ConfigService } from '../../../services/config.service';
import { TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('AnalyticsComponent', () => {
  let fixture: ComponentFixture<AnalyticsComponent>;
  let httpTestingController: HttpTestingController;
  let harness: AnalyticsComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL, configuration: {} } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  async function setup(response: AnalyticsDashboardsResponse = fakeAnalyticsDashboardsResponse()) {
    fixture.detectChanges();
    httpTestingController
      .expectOne(r => r.url === `${TESTING_BASE_URL}/analytics/dashboards` && r.params.get('page') === '1' && r.params.get('size') === '20')
      .flush(response);
    await fixture.whenStable();
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AnalyticsComponentHarness);
  }

  it('should_set_breadcrumbs_for_analytics_list', async () => {
    await setup();
    const breadcrumbService = TestBed.inject(BreadcrumbService);
    expect(breadcrumbService.breadcrumbs()).toEqual([analyticsListBreadcrumb()]);
  });

  describe('populated dashboard list', () => {
    it('should_show_analytics_title', async () => {
      await setup();
      expect(await harness.getTitle()).toContain('Analytics');
    });

    it('should_show_dashboard_cards', async () => {
      await setup(
        fakeAnalyticsDashboardsResponse({
          data: [
            fakeDashboard({ id: 'dash-1', name: 'HTTP Overview', labels: { type: 'http' } }),
            fakeDashboard({ id: 'dash-2', name: 'API Performance', labels: {} }),
          ],
        }),
      );
      const cards = await harness.getCards();
      expect(cards.length).toBe(2);
      expect(await cards[0].getTitle()).toContain('HTTP Overview');
    });

    it('should_navigate_to_dashboard_on_card_select', async () => {
      await setup();
      const router = TestBed.inject(Router);
      const navigateSpy = jest.spyOn(router, 'navigate');

      fixture.componentInstance.navigateToDashboard('dash-1');

      expect(navigateSpy).toHaveBeenCalledWith(['dash-1'], expect.anything());
    });
  });

  describe('pin/unpin', () => {
    beforeEach(async () => {
      localStorage.clear();
      await setup(
        fakeAnalyticsDashboardsResponse({
          data: [
            fakeDashboard({ id: 'dash-1', name: 'Dashboard 1' }),
            fakeDashboard({ id: 'dash-2', name: 'Dashboard 2' }),
            fakeDashboard({ id: 'dash-3', name: 'Dashboard 3' }),
            fakeDashboard({ id: 'dash-4', name: 'Dashboard 4' }),
            fakeDashboard({ id: 'dash-5', name: 'Dashboard 5' }),
          ],
        }),
      );
    });

    afterEach(() => {
      localStorage.clear();
    });

    it('should_toggle_pin_and_persist_to_local_storage', () => {
      fixture.componentInstance.togglePin('dash-1');
      expect(fixture.componentInstance.pinnedIds()).toEqual(['dash-1']);
      expect(JSON.parse(localStorage.getItem('analytics-pinned-dashboards')!)).toEqual(['dash-1']);
      // Flush the pinnedResource getById call triggered by signal change
      httpTestingController.match(r => r.url.includes('/analytics/dashboards/dash-1'));
    });

    it('should_unpin_when_already_pinned', () => {
      fixture.componentInstance.togglePin('dash-1');
      httpTestingController.match(r => r.url.includes('/analytics/dashboards/'));
      fixture.componentInstance.togglePin('dash-1');
      expect(fixture.componentInstance.pinnedIds()).toEqual([]);
    });

    it('should_show_pinned_dashboards_in_pinned_row', async () => {
      fixture.componentInstance.togglePin('dash-1');
      fixture.detectChanges();
      httpTestingController
        .expectOne(r => r.url === `${TESTING_BASE_URL}/analytics/dashboards/dash-1`)
        .flush(fakeDashboard({ id: 'dash-1', name: 'Dashboard 1' }));
      await fixture.whenStable();
      fixture.detectChanges();
      const pinnedRow = await harness.getPinnedRow();
      expect(pinnedRow).toBeTruthy();
    });

    it('should_not_allow_pinning_more_than_4', () => {
      fixture.componentInstance.togglePin('dash-1');
      fixture.componentInstance.togglePin('dash-2');
      fixture.componentInstance.togglePin('dash-3');
      fixture.componentInstance.togglePin('dash-4');
      expect(fixture.componentInstance.canPinMore()).toBe(false);
      httpTestingController.match(r => r.url.includes('/analytics/dashboards/'));
    });

    it('should_allow_pinning_when_under_limit', () => {
      fixture.componentInstance.togglePin('dash-1');
      fixture.componentInstance.togglePin('dash-2');
      fixture.componentInstance.togglePin('dash-3');
      expect(fixture.componentInstance.canPinMore()).toBe(true);
      httpTestingController.match(r => r.url.includes('/analytics/dashboards/'));
    });
  });

  describe('empty dashboard list', () => {
    it('should_show_empty_state', async () => {
      await setup(fakeAnalyticsDashboardsResponse({ data: [] }));
      expect(await harness.isEmptyStateDisplayed()).toBe(true);
    });
  });

  describe('partial response', () => {
    it('should_apply_fallbacks_when_metadata_is_missing', async () => {
      await setup({ data: [] });
      const vm = fixture.componentInstance['dashboardPaginator']();
      expect(vm.page).toBe(1);
      expect(vm.totalResults).toBe(0);
    });

    it('should_default_data_to_empty_array_when_field_missing', async () => {
      await setup({});
      expect(fixture.componentInstance['dashboardPaginator']().data).toEqual([]);
    });
  });

  describe('failed dashboard list', () => {
    it('should_show_inline_error_alert_when_request_fails', async () => {
      fixture.detectChanges();
      httpTestingController
        .expectOne(r => r.url === `${TESTING_BASE_URL}/analytics/dashboards`)
        .error(new ProgressEvent('Network error'), { status: 500, statusText: 'Server Error' });
      await fixture.whenStable();
      fixture.detectChanges();
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AnalyticsComponentHarness);

      const errorMessage = await harness.getErrorMessage();
      expect(errorMessage).toBeTruthy();
      expect(errorMessage).toContain('error occurred');
    });
  });
});
