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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { subscriptionListBreadcrumb } from './subscription-breadcrumbs';
import SubscriptionsComponent from './subscriptions.component';
import { SubscriptionsComponentHarness } from './subscriptions.component.harness';
import { ApplicationsResponse } from '../../../entities/application/application';
import { fakeApplication, fakeApplicationsResponse } from '../../../entities/application/application.fixture';
import { SubscriptionConsumerStatusEnum, SubscriptionStatusEnum } from '../../../entities/subscription';
import { fakeSubscriptionResponse } from '../../../entities/subscription/subscription.fixture';
import { SubscriptionsResponse } from '../../../entities/subscription/subscriptions-response';
import { BreadcrumbService } from '../../../services/breadcrumb.service';

const emptySubscriptions = { data: [], links: { self: '' }, metadata: {} };

@Component({ template: '' })
class SubscriptionDetailsRouteStubComponent {}

describe('SubscriptionsComponent', () => {
  let fixture: ComponentFixture<SubscriptionsComponent>;
  let harness: SubscriptionsComponentHarness;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([{ path: ':subscriptionId', component: SubscriptionDetailsRouteStubComponent }]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionsComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Detects changes, flushes applications + subscriptions, waits for stability. */
  async function setup(subsResponse: SubscriptionsResponse = emptySubscriptions, appsResponse: ApplicationsResponse = { data: [] }) {
    fixture.detectChanges();
    http.expectOne(req => req.url.includes('/applications')).flush(appsResponse);
    http.expectOne(req => req.url.includes('/subscriptions')).flush(subsResponse);
    await fixture.whenStable();
  }

  /** Flushes pending subscription requests. */
  function flushSubscriptions(response: SubscriptionsResponse = fakeSubscriptionResponse(), controller: HttpTestingController = http) {
    controller.match(req => req.url.includes('/subscriptions')).forEach(req => req.flush(response));
  }

  /** Runs promise while periodically flushing subscription requests (for harness interactions that trigger refetch). */
  async function withFlush<T>(promise: Promise<T>, controller: HttpTestingController = http): Promise<T> {
    const id = setInterval(() => flushSubscriptions(fakeSubscriptionResponse(), controller), 50);
    try {
      return await promise;
    } finally {
      clearInterval(id);
    }
  }

  async function getHarness() {
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionsComponentHarness);
    return harness;
  }

  it('should create', async () => {
    await setup();
    await getHarness();
    expect(fixture.componentInstance).toBeTruthy();
    expect(await harness.host()).toBeTruthy();
  });

  it('should set breadcrumbs for the subscriptions list', async () => {
    await setup();
    const breadcrumbService = TestBed.inject(BreadcrumbService);
    expect(breadcrumbService.breadcrumbs()).toEqual([subscriptionListBreadcrumb()]);
  });

  it('should show empty state when no subscriptions', async () => {
    await setup();
    await getHarness();
    expect(await harness.isEmptyStateDisplayed()).toBe(true);
  });

  it('should show table when subscriptions exist', async () => {
    await setup(fakeSubscriptionResponse());
    await getHarness();
    expect(await harness.isEmptyStateDisplayed()).toBe(false);
  });

  it('should map response rows with metadata names', async () => {
    const response: SubscriptionsResponse = {
      data: [
        {
          id: 'sub-1',
          api: 'api-1',
          reference_id: 'api-1',
          reference_type: 'API',
          application: 'app-1',
          plan: 'plan-1',
          status: 'ACCEPTED',
          created_at: '2026-02-03T23:00:00Z',
          start_at: '2026-02-04T23:00:00Z',
          end_at: '2027-02-04T23:00:00Z',
          consumerStatus: SubscriptionConsumerStatusEnum.STARTED,
        },
      ],
      metadata: {
        'api-1': { name: 'API One', apiVersion: '1' },
        'app-1': { name: 'App One' },
        'plan-1': { name: 'Plan One' },
      },
      links: { self: '' },
    };
    await setup(response);

    expect(fixture.componentInstance.rows().length).toBe(1);
    expect(fixture.componentInstance.rows()[0]).toEqual({
      id: 'sub-1',
      targetName: 'API One',
      targetType: 'API',
      plan: 'Plan One',
      application: 'App One',
      startAt: '2026-02-04T23:00:00Z',
      endAt: '2027-02-04T23:00:00Z',
      status: 'Accepted',
    });
  });

  it('should map API and API Product subscriptions using their reference identity', async () => {
    const response: SubscriptionsResponse = {
      data: [
        {
          id: 'api-subscription',
          api: 'legacy-api-id',
          reference_id: 'api-id',
          reference_type: 'API',
          application: 'app-id',
          plan: 'api-plan-id',
          status: 'ACCEPTED',
          start_at: '2026-02-01T00:00:00Z',
          consumerStatus: SubscriptionConsumerStatusEnum.STARTED,
        },
        {
          id: 'product-subscription',
          reference_id: 'product-id',
          reference_type: 'API_PRODUCT',
          application: 'app-id',
          plan: 'product-plan-id',
          status: 'PENDING',
          end_at: '2027-02-01T00:00:00Z',
          consumerStatus: SubscriptionConsumerStatusEnum.STARTED,
        },
      ],
      metadata: {
        'api-id': { name: 'Payments API' },
        'product-id': { name: 'Payments Product' },
        'app-id': { name: 'Consumer App' },
        'api-plan-id': { name: 'API Plan' },
        'product-plan-id': { name: 'Product Plan' },
        paginateMetaData: { totalElements: 2 },
      },
      links: { self: '' },
    };

    await setup(response);

    expect(fixture.componentInstance.rows()).toEqual([
      expect.objectContaining({ targetName: 'Payments API', targetType: 'API' }),
      expect.objectContaining({ targetName: 'Payments Product', targetType: 'API Product' }),
    ]);
    expect(fixture.componentInstance.totalElements()).toBe(2);
  });

  it('should show a safe fallback when subscription target metadata is unavailable', async () => {
    await setup(
      fakeSubscriptionResponse({
        data: [
          {
            id: 'product-subscription',
            reference_id: 'deleted-product-id',
            reference_type: 'API_PRODUCT',
            application: 'app-id',
            plan: 'plan-id',
            status: 'CLOSED',
            consumerStatus: SubscriptionConsumerStatusEnum.STOPPED,
          },
        ],
        metadata: {},
      }),
    );

    expect(fixture.componentInstance.rows()[0].targetName).toBe('Unavailable API Product');
  });

  it('should display the backend subscription count', async () => {
    await setup(
      fakeSubscriptionResponse({
        metadata: {
          paginateMetaData: { totalElements: 37 },
        },
      }),
    );
    fixture.detectChanges();
    await getHarness();

    expect(await harness.getCountText()).toBe('37 subscriptions');
  });

  it('should display an accessible error when subscriptions cannot be loaded', async () => {
    fixture.detectChanges();
    http.expectOne(req => req.url.includes('/applications')).flush({ data: [] });
    http.expectOne(req => req.url.includes('/subscriptions')).flush('error', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    await getHarness();

    expect(await harness.isErrorDisplayed()).toBe(true);
  });

  it('should navigate an API Product subscription row through the existing subscription details route', async () => {
    await setup(
      fakeSubscriptionResponse({
        data: [
          {
            id: 'product-subscription',
            reference_id: 'product-id',
            reference_type: 'API_PRODUCT',
            application: 'app-id',
            plan: 'plan-id',
            status: 'ACCEPTED',
            consumerStatus: SubscriptionConsumerStatusEnum.STARTED,
          },
        ],
        metadata: { 'product-id': { name: 'Payments Product' } },
      }),
    );
    fixture.detectChanges();
    await getHarness();

    await harness.clickTableRow(0);

    expect(TestBed.inject(Router).url).toBe('/product-subscription');
  });

  it('should have default filters and pagination', async () => {
    await setup();

    expect(fixture.componentInstance.currentPage()).toBe(1);
    expect(fixture.componentInstance.pageSize()).toBe(10);
    expect(fixture.componentInstance.filters().query).toBe('');
    expect(fixture.componentInstance.typeFilter.value).toEqual([]);
    expect(fixture.componentInstance.applicationFilter.value).toBeNull();
    expect(fixture.componentInstance.statusFilter.value).toEqual([]);
  });

  it('should explicitly request API and API Product subscriptions by default', async () => {
    fixture.detectChanges();
    http.expectOne(req => req.url.includes('/applications')).flush({ data: [] });
    const request = http.expectOne(req => req.url.includes('/subscriptions'));

    expect(request.request.params.getAll('referenceTypes')).toEqual(['API', 'API_PRODUCT']);
    request.flush(emptySubscriptions);
    await fixture.whenStable();
  });

  it('should clear filters and reset page', async () => {
    await setup(fakeSubscriptionResponse(), fakeApplicationsResponse({ data: [fakeApplication({ id: 'app-1', name: 'App One' })] }));
    await getHarness();

    await TestBed.inject(Router).navigate([], {
      queryParams: {
        query: 'payments',
        referenceTypes: ['API_PRODUCT'],
        applicationIds: ['app-1'],
        statuses: SubscriptionStatusEnum.ACCEPTED,
        page: 3,
      },
      queryParamsHandling: 'merge',
    });
    await fixture.whenStable();
    flushSubscriptions();

    fixture.componentInstance.clearFilters();
    await fixture.whenStable();
    flushSubscriptions(emptySubscriptions as SubscriptionsResponse);

    expect(fixture.componentInstance.filters().query).toBe('');
    expect(fixture.componentInstance.typeFilter.value).toEqual([]);
    expect(fixture.componentInstance.applicationFilter.value).toBeNull();
    expect(fixture.componentInstance.statusFilter.value).toEqual([]);
    expect(fixture.componentInstance.currentPage()).toBe(1);
  });

  it('should init filters from URL query params', async () => {
    const params = { query: 'payment', referenceTypes: ['API_PRODUCT'], statuses: 'ACCEPTED', page: '2', size: '20' };
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [SubscriptionsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams: params }, queryParams: of(params) } },
      ],
    }).compileComponents();

    const f = TestBed.createComponent(SubscriptionsComponent);
    const c = TestBed.inject(HttpTestingController);
    f.detectChanges();
    c.expectOne(req => req.url.includes('/applications')).flush({ data: [] });
    c.expectOne(req => req.url.includes('/subscriptions')).flush(emptySubscriptions);

    expect(f.componentInstance.filters().query).toBe('payment');
    expect(f.componentInstance.typeFilter.value).toEqual(['API_PRODUCT']);
    expect(f.componentInstance.statusFilter.value).toEqual(['ACCEPTED']);
    expect(f.componentInstance.currentPage()).toBe(2);
    expect(f.componentInstance.pageSize()).toBe(20);
    c.verify();
  });

  it('should update page on page change', async () => {
    await setup(fakeSubscriptionResponse());
    fixture.componentInstance.onPageChange(3);
    await fixture.whenStable();
    expect(fixture.componentInstance.currentPage()).toBe(3);
  });

  it('should reset to page 1 on page size change', async () => {
    await setup(fakeSubscriptionResponse());
    fixture.componentInstance.onPageChange(5);
    await fixture.whenStable();
    fixture.componentInstance.onPageSizeChange(25);
    await fixture.whenStable();
    expect(fixture.componentInstance.pageSize()).toBe(25);
    expect(fixture.componentInstance.currentPage()).toBe(1);
  });

  it('should expose Type, Application, and Status filter dropdowns via harness', async () => {
    await setup(fakeSubscriptionResponse());
    await getHarness();
    const type = await harness.getTypeFilter();
    const app = await harness.getApplicationFilter();
    const status = await harness.getStatusFilter();
    expect(type).toBeTruthy();
    expect(app).toBeTruthy();
    expect(status).toBeTruthy();
    expect(await type.getTriggerText()).toBeTruthy();
  });

  it('should update status filter when selecting via harness', async () => {
    await setup(fakeSubscriptionResponse());
    await getHarness();
    await withFlush(harness.selectStatusFilter(['Accepted']));
    fixture.detectChanges();
    expect(fixture.componentInstance.statusFilter.value).toContain(SubscriptionStatusEnum.ACCEPTED);
  });

  it('should update application filter when selecting via harness', async () => {
    await setup(fakeSubscriptionResponse(), fakeApplicationsResponse({ data: [fakeApplication({ id: 'app-1', name: 'App One' })] }));
    await getHarness();
    await withFlush(harness.selectApplicationFilter(0));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.applicationFilter.value).toContain('app-1');
  });

  it('should update target type filter when selecting via harness', async () => {
    await setup(fakeSubscriptionResponse());
    await getHarness();

    await withFlush(harness.selectTypeFilter(['API Product']));
    fixture.detectChanges();

    expect(fixture.componentInstance.typeFilter.value).toEqual(['API_PRODUCT']);
  });

  it('should not navigate when form values match URL filters', async () => {
    await setup();
    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate');

    (fixture.componentInstance as unknown as { syncUrlToForm: () => void }).syncUrlToForm();

    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('should navigate with normalized filter params when form values change', async () => {
    await setup();
    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate');

    fixture.componentInstance.typeFilter.setValue(['API_PRODUCT'], { emitEvent: false });
    fixture.componentInstance.applicationFilter.setValue(['app-1'], { emitEvent: false });
    fixture.componentInstance.statusFilter.setValue([SubscriptionStatusEnum.ACCEPTED], { emitEvent: false });
    (fixture.componentInstance as unknown as { syncUrlToForm: () => void }).syncUrlToForm();

    expect(navigateSpy).toHaveBeenCalledWith([], {
      relativeTo: TestBed.inject(ActivatedRoute),
      queryParams: {
        query: null,
        referenceTypes: ['API_PRODUCT'],
        applicationIds: ['app-1'],
        statuses: [SubscriptionStatusEnum.ACCEPTED],
        page: 1,
        size: 10,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  });

  it('should reset pagination when search changes', async () => {
    await setup(fakeSubscriptionResponse());
    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate');

    fixture.componentInstance.onSearchTermChange('payment');

    expect(navigateSpy).toHaveBeenCalledWith([], {
      relativeTo: TestBed.inject(ActivatedRoute),
      queryParams: { query: 'payment', page: 1 },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  });

  it('should ignore invalid statuses from URL query params', async () => {
    const params = { statuses: ['ACCEPTED', 'INVALID'], page: '1', size: '10' };
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [SubscriptionsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams: params }, queryParams: of(params) } },
      ],
    }).compileComponents();

    const f = TestBed.createComponent(SubscriptionsComponent);
    const c = TestBed.inject(HttpTestingController);
    f.detectChanges();
    c.expectOne(req => req.url.includes('/applications')).flush({ data: [] });
    c.expectOne(req => req.url.includes('/subscriptions')).flush(emptySubscriptions);

    expect(f.componentInstance.statusFilter.value).toEqual([SubscriptionStatusEnum.ACCEPTED]);
    c.verify();
  });

  it('should ignore invalid reference types from URL query params', async () => {
    const params = { referenceTypes: ['API_PRODUCT', 'UNKNOWN'], page: '1', size: '10' };
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [SubscriptionsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams: params }, queryParams: of(params) } },
      ],
    }).compileComponents();

    const f = TestBed.createComponent(SubscriptionsComponent);
    const c = TestBed.inject(HttpTestingController);
    f.detectChanges();
    c.expectOne(req => req.url.includes('/applications')).flush({ data: [] });
    c.expectOne(req => req.url.includes('/subscriptions')).flush(emptySubscriptions);

    expect(f.componentInstance.typeFilter.value).toEqual(['API_PRODUCT']);
    c.verify();
  });

  it('hasSubscriptions is false when no data and no filters', async () => {
    await setup();
    expect(fixture.componentInstance.hasSubscriptions()).toBe(false);
  });

  it('hasSubscriptions is true when data exists', async () => {
    await setup(fakeSubscriptionResponse());
    expect(fixture.componentInstance.hasSubscriptions()).toBe(true);
  });
});
