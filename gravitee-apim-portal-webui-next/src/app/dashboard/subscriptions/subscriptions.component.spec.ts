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
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import SubscriptionsComponent from './subscriptions.component';
import { SubscriptionsComponentHarness } from './subscriptions.component.harness';
import { ApplicationsResponse } from '../../../entities/application/application';
import { fakeApplication, fakeApplicationsResponse } from '../../../entities/application/application.fixture';
import { SubscriptionConsumerStatusEnum, SubscriptionStatusEnum } from '../../../entities/subscription';
import { fakeSubscriptionResponse } from '../../../entities/subscription/subscription.fixture';
import { SubscriptionsResponse } from '../../../entities/subscription/subscriptions-response';
import { ApiService } from '../../../services/api.service';

const emptySubscriptions = { data: [], links: { self: '' }, metadata: {} };

describe('SubscriptionsComponent', () => {
  let fixture: ComponentFixture<SubscriptionsComponent>;
  let harness: SubscriptionsComponentHarness;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), provideRouter([])],
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
          application: 'app-1',
          plan: 'plan-1',
          status: 'ACCEPTED',
          created_at: '2026-02-03T23:00:00Z',
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
      api: 'API One',
      plan: 'Plan One',
      application: 'App One',
      created_at: '2026-02-03T23:00:00Z',
      status: 'Accepted',
    });
  });

  it('should have default filters and pagination', async () => {
    await setup();

    expect(fixture.componentInstance.currentPage()).toBe(1);
    expect(fixture.componentInstance.pageSize()).toBe(10);
    expect(fixture.componentInstance.apiFilter.value).toBeNull();
    expect(fixture.componentInstance.applicationFilter.value).toBeNull();
    expect(fixture.componentInstance.statusFilter.value).toEqual([]);
  });

  it('should clear filters and reset page', async () => {
    await setup(fakeSubscriptionResponse(), fakeApplicationsResponse({ data: [fakeApplication({ id: 'app-1', name: 'App One' })] }));
    await getHarness();

    await TestBed.inject(Router).navigate([], {
      queryParams: { apiIds: ['api-1'], applicationIds: ['app-1'], statuses: SubscriptionStatusEnum.ACCEPTED, page: 3 },
      queryParamsHandling: 'merge',
    });
    await fixture.whenStable();
    flushSubscriptions();

    fixture.componentInstance.clearFilters();
    await fixture.whenStable();
    flushSubscriptions(emptySubscriptions as SubscriptionsResponse);

    expect(fixture.componentInstance.apiFilter.value).toBeNull();
    expect(fixture.componentInstance.applicationFilter.value).toBeNull();
    expect(fixture.componentInstance.statusFilter.value).toEqual([]);
    expect(fixture.componentInstance.currentPage()).toBe(1);
  });

  it('should init filters from URL query params', async () => {
    const params = { apiIds: ['api-1'], statuses: 'ACCEPTED', page: '2', size: '20' };
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

    expect(f.componentInstance.apiFilter.value).toEqual(['api-1']);
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

  it('should expose API, Application, Status filter dropdowns via harness', async () => {
    await setup(fakeSubscriptionResponse());
    await getHarness();
    const api = await harness.getApiFilter();
    const app = await harness.getApplicationFilter();
    const status = await harness.getStatusFilter();
    expect(api).toBeTruthy();
    expect(app).toBeTruthy();
    expect(status).toBeTruthy();
    expect(await api.getTriggerText()).toBeTruthy();
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

  it('should update API filter when selecting via harness', async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [SubscriptionsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        {
          provide: ApiService,
          useValue: {
            search: () =>
              of({
                data: [{ id: 'api-1', name: 'API One' }],
                metadata: { pagination: { current_page: 1, total_pages: 1 } },
              }),
          },
        },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(SubscriptionsComponent);
    const c = TestBed.inject(HttpTestingController);
    f.detectChanges();
    c.expectOne(req => req.url.includes('/applications')).flush({ data: [] });
    c.expectOne(req => req.url.includes('/subscriptions')).flush(fakeSubscriptionResponse());
    await f.whenStable();

    const h = await TestbedHarnessEnvironment.harnessForFixture(f, SubscriptionsComponentHarness);
    await withFlush(h.selectApiFilter(0), c);
    f.detectChanges();
    expect(f.componentInstance.apiFilter.value).toContain('api-1');
    c.verify();
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

    fixture.componentInstance.apiFilter.setValue(['api-1'], { emitEvent: false });
    fixture.componentInstance.applicationFilter.setValue(['app-1'], { emitEvent: false });
    fixture.componentInstance.statusFilter.setValue([SubscriptionStatusEnum.ACCEPTED], { emitEvent: false });
    (fixture.componentInstance as unknown as { syncUrlToForm: () => void }).syncUrlToForm();

    expect(navigateSpy).toHaveBeenCalledWith([], {
      relativeTo: TestBed.inject(ActivatedRoute),
      queryParams: {
        apiIds: ['api-1'],
        applicationIds: ['app-1'],
        statuses: [SubscriptionStatusEnum.ACCEPTED],
        page: 1,
        size: 10,
      },
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

  it('hasSubscriptions is false when no data and no filters', async () => {
    await setup();
    expect(fixture.componentInstance.hasSubscriptions()).toBe(false);
  });

  it('hasSubscriptions is true when data exists', async () => {
    await setup(fakeSubscriptionResponse());
    expect(fixture.componentInstance.hasSubscriptions()).toBe(true);
  });
});
