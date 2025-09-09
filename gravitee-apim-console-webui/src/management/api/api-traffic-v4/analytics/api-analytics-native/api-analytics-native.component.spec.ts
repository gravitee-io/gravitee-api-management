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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { of } from 'rxjs';

import { ApiAnalyticsNativeHarness } from './api-analytics-native.component.harness';
import { ApiAnalyticsNativeComponent } from './api-analytics-native.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakePagedResult, fakePlanV4, PlanV4 } from '../../../../../entities/management-api-v2';

describe('ApiAnalyticsNativeComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiAnalyticsNativeComponent>;
  let componentHarness: ApiAnalyticsNativeHarness;
  let httpTestingController: HttpTestingController;

  const initComponent = async (queryParams = {}) => {
    TestBed.configureTestingModule({
      imports: [ApiAnalyticsNativeComponent, OwlNativeDateTimeModule, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: queryParams,
            },
            queryParams: of(queryParams),
            params: of({ apiId: API_ID }),
          },
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiAnalyticsNativeComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsNativeHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('GIVEN an API with analytics enabled', () => {
    beforeEach(async () => {
      await initComponent();
      expectPlanList();
    });

    it('should display analytics widgets in empty state', async () => {
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeFalsy();
    });

    it('should refresh when filters are applied', async () => {
      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.clickRefresh();
    });
  });

  describe('Query parameters', () => {
    const plan1 = fakePlanV4({ id: '1', name: 'plan 1' });
    const plan2 = fakePlanV4({ id: '2', name: 'plan 2' });

    it('should use default time range when no query params provided', async () => {
      await initComponent();
      expectPlanList();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedValue = await filtersBar.getSelectedPeriod();

      expect(selectedValue).toEqual('Last day');
    });

    it('should use custom time range from query params', async () => {
      await initComponent({ period: '1M' });
      expectPlanList();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedValue = await filtersBar.getSelectedPeriod();

      expect(selectedValue).toEqual('Last month');
    });

    it('should use and select plans from query params', async () => {
      await initComponent({ plans: '1' });

      expectPlanList([plan1, plan2]);

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedValues = await filtersBar.getSelectedPlans();

      expect(selectedValues).toEqual(['1']);
    });

    it('should update the URL query params when a plan is selected', async () => {
      await initComponent();

      expectPlanList([plan1, plan2]);

      const router = TestBed.inject(Router);
      const routerSpy = jest.spyOn(router, 'navigate');

      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.selectPlan('plan 2');

      expect(routerSpy).toHaveBeenCalledWith([], {
        queryParams: {
          period: '1d',
          plans: '2',
        },
        queryParamsHandling: 'replace',
      });
    });
  });

  function expectPlanList(plans: PlanV4[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=9999&statuses=PUBLISHED,DEPRECATED,CLOSED`,
        method: 'GET',
      })
      .flush(fakePagedResult(plans));
    fixture.detectChanges();
  }
});
