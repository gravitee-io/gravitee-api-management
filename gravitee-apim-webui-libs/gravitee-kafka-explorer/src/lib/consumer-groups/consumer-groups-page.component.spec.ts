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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';

import { ConsumerGroupsPageComponent } from './consumer-groups-page.component';
import { ConsumerGroupsHarness } from './consumer-groups.harness';
import { fakeConsumerGroupSummary, fakeListConsumerGroupsResponse, fakePagination } from '../models/kafka-cluster.fixture';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';

describe('ConsumerGroupsPageComponent', () => {
  let fixture: ComponentFixture<ConsumerGroupsPageComponent>;
  let httpTesting: HttpTestingController;
  let loader: HarnessLoader;

  const router = { navigate: jest.fn() };
  const route = { snapshot: { params: {} } };

  beforeEach(async () => {
    router.navigate.mockClear();

    await TestBed.configureTestingModule({
      imports: [ConsumerGroupsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        KafkaExplorerStore,
        provideNoopAnimations(),
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: route },
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    const store = TestBed.inject(KafkaExplorerStore);
    store.baseURL.set('/api/v2');
    store.clusterId.set('test-cluster-id');

    fixture = TestBed.createComponent(ConsumerGroupsPageComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushConsumerGroups(response = fakeListConsumerGroupsResponse()) {
    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/list-consumer-groups').flush(response);
    fixture.detectChanges();
  }

  it('should call listConsumerGroups on init', () => {
    const req = httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/list-consumer-groups');
    expect(req.request.body).toEqual({ clusterId: 'test-cluster-id' });
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('perPage')).toBe('25');
    req.flush(fakeListConsumerGroupsResponse());
  });

  it('should render consumer groups in the table', async () => {
    flushConsumerGroups();

    const harness = await loader.getHarness(ConsumerGroupsHarness);
    const rows = await harness.getRowsData();
    expect(rows.length).toBe(3);
    expect(rows[0]).toEqual(
      expect.objectContaining({
        groupId: 'my-group',
        state: 'STABLE',
        membersCount: '2',
      }),
    );
    expect(rows[1]['groupId']).toBe('orders-group');
  });

  it('should render an empty table when no consumer groups', async () => {
    flushConsumerGroups(fakeListConsumerGroupsResponse({ data: [], pagination: fakePagination({ totalCount: 0, pageItemsCount: 0 }) }));

    const harness = await loader.getHarness(ConsumerGroupsHarness);
    expect(await harness.getRowCount()).toBe(0);
  });

  it('should show progress bar when loading', async () => {
    const harness = await loader.getHarness(ConsumerGroupsHarness);
    expect(await harness.isLoading()).toBe(true);

    flushConsumerGroups();
  });

  it('should not show progress bar after loading completes', async () => {
    flushConsumerGroups();

    const harness = await loader.getHarness(ConsumerGroupsHarness);
    expect(await harness.isLoading()).toBe(false);
  });

  it('should show paginator with correct range', async () => {
    flushConsumerGroups(
      fakeListConsumerGroupsResponse({
        data: [fakeConsumerGroupSummary()],
        pagination: fakePagination({ totalCount: 50, perPage: 10, pageItemsCount: 10 }),
      }),
    );

    const harness = await loader.getHarness(ConsumerGroupsHarness);
    const rangeLabel = await harness.getRangeLabel();
    expect(rangeLabel).toContain('1');
    expect(rangeLabel).toContain('50');
  });

  it('should reload consumer groups after filter with debounce', fakeAsync(async () => {
    flushConsumerGroups();

    const harness = await loader.getHarness(ConsumerGroupsHarness);
    await harness.setFilter('my');
    tick(300);

    httpTesting
      .expectOne(req => req.url === '/api/v2/kafka-explorer/list-consumer-groups' && req.body.nameFilter === 'my')
      .flush(fakeListConsumerGroupsResponse());
  }));

  it('should reload consumer groups on page change', async () => {
    flushConsumerGroups(fakeListConsumerGroupsResponse({ pagination: fakePagination({ totalCount: 50, perPage: 10 }) }));

    const harness = await loader.getHarness(ConsumerGroupsHarness);
    await harness.goToNextPage();

    httpTesting
      .expectOne(req => req.url === '/api/v2/kafka-explorer/list-consumer-groups' && req.params.get('page') === '2')
      .flush(fakeListConsumerGroupsResponse());
  });

  it('should navigate to consumer group detail on group select', async () => {
    flushConsumerGroups();

    const harness = await loader.getHarness(ConsumerGroupsHarness);
    const rows = await harness.getRows();
    await (await rows[0].host()).click();

    expect(router.navigate).toHaveBeenCalledWith(['my-group'], { relativeTo: route });
  });
});
