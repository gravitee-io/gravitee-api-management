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

import { TopicsPageComponent } from './topics-page.component';
import { TopicsHarness } from './topics.harness';
import { fakeKafkaTopic, fakeListTopicsResponse, fakePagination } from '../models/kafka-cluster.fixture';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';

describe('TopicsPageComponent', () => {
  let fixture: ComponentFixture<TopicsPageComponent>;
  let httpTesting: HttpTestingController;
  let loader: HarnessLoader;

  const router = { navigate: jest.fn() };
  const route = { snapshot: { params: {} } };

  beforeEach(async () => {
    router.navigate.mockClear();

    await TestBed.configureTestingModule({
      imports: [TopicsPageComponent],
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

    fixture = TestBed.createComponent(TopicsPageComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushTopics(response = fakeListTopicsResponse()) {
    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/list-topics').flush(response);
    fixture.detectChanges();
  }

  it('should call listTopics on init', () => {
    const req = httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/list-topics');
    expect(req.request.body).toEqual({ clusterId: 'test-cluster-id' });
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('perPage')).toBe('10');
    req.flush(fakeListTopicsResponse());
  });

  it('should render topics in the table', async () => {
    flushTopics();

    const harness = await loader.getHarness(TopicsHarness);
    const rows = await harness.getRowsData();
    expect(rows.length).toBe(3);
    expect(rows[0]).toEqual(
      expect.objectContaining({
        name: 'my-topic',
        partitionCount: '3',
        replicationFactor: '2',
        underReplicatedCount: '0',
      }),
    );
    expect(rows[1]['name']).toBe('orders');
  });

  it('should show warning badge for under-replicated partitions', async () => {
    flushTopics(
      fakeListTopicsResponse({
        data: [fakeKafkaTopic({ name: 'topic-1', underReplicatedCount: 2 })],
        pagination: fakePagination({ totalCount: 1, pageItemsCount: 1 }),
      }),
    );

    const harness = await loader.getHarness(TopicsHarness);
    const rows = await harness.getRowsData();
    expect(rows[0]['underReplicatedCount']).toContain('2');
  });

  it('should show Internal badge for internal topics', async () => {
    flushTopics(
      fakeListTopicsResponse({
        data: [fakeKafkaTopic({ name: '__consumer_offsets', internal: true })],
        pagination: fakePagination({ totalCount: 1, pageItemsCount: 1 }),
      }),
    );

    const harness = await loader.getHarness(TopicsHarness);
    const rows = await harness.getRowsData();
    expect(rows[0]['name']).toContain('__consumer_offsets');
    expect(rows[0]['name']).toContain('Internal');
  });

  it('should not show Internal badge for non-internal topics', async () => {
    flushTopics(
      fakeListTopicsResponse({
        data: [fakeKafkaTopic({ name: 'my-topic', internal: false })],
        pagination: fakePagination({ totalCount: 1, pageItemsCount: 1 }),
      }),
    );

    const harness = await loader.getHarness(TopicsHarness);
    const rows = await harness.getRowsData();
    expect(rows[0]['name']).toBe('my-topic');
    expect(rows[0]['name']).not.toContain('Internal');
  });

  it('should display size and messageCount columns', async () => {
    flushTopics(
      fakeListTopicsResponse({
        data: [fakeKafkaTopic({ name: 'my-topic', size: 1048576, messageCount: 1500 })],
        pagination: fakePagination({ totalCount: 1, pageItemsCount: 1 }),
      }),
    );

    const harness = await loader.getHarness(TopicsHarness);
    const rows = await harness.getRowsData();
    expect(rows[0]['size']).toBe('1 MB');
    expect(rows[0]['messageCount']).toContain('1,500');
  });

  it('should display dash for undefined size and messageCount', async () => {
    flushTopics(
      fakeListTopicsResponse({
        data: [fakeKafkaTopic({ name: 'my-topic', size: undefined, messageCount: undefined })],
        pagination: fakePagination({ totalCount: 1, pageItemsCount: 1 }),
      }),
    );

    const harness = await loader.getHarness(TopicsHarness);
    const rows = await harness.getRowsData();
    expect(rows[0]['size']).toBe('-');
    expect(rows[0]['messageCount']).toBe('-');
  });

  it('should render an empty table when no topics', async () => {
    flushTopics(fakeListTopicsResponse({ data: [], pagination: fakePagination({ totalCount: 0, pageItemsCount: 0 }) }));

    const harness = await loader.getHarness(TopicsHarness);
    expect(await harness.getRowCount()).toBe(0);
  });

  it('should show progress bar when loading', async () => {
    const harness = await loader.getHarness(TopicsHarness);
    expect(await harness.isLoading()).toBe(true);

    flushTopics();
  });

  it('should not show progress bar after loading completes', async () => {
    flushTopics();

    const harness = await loader.getHarness(TopicsHarness);
    expect(await harness.isLoading()).toBe(false);
  });

  it('should show paginator with correct range', async () => {
    flushTopics(
      fakeListTopicsResponse({
        data: [fakeKafkaTopic()],
        pagination: fakePagination({ totalCount: 50, perPage: 10, pageItemsCount: 10 }),
      }),
    );

    const harness = await loader.getHarness(TopicsHarness);
    const rangeLabel = await harness.getRangeLabel();
    expect(rangeLabel).toContain('1');
    expect(rangeLabel).toContain('50');
  });

  it('should reload topics after filter with debounce', fakeAsync(async () => {
    flushTopics();

    const harness = await loader.getHarness(TopicsHarness);
    await harness.setFilter('order');
    tick(300);

    httpTesting
      .expectOne(req => req.url === '/api/v2/kafka-explorer/list-topics' && req.body.nameFilter === 'order')
      .flush(fakeListTopicsResponse());
  }));

  it('should reload topics on page change', async () => {
    flushTopics(fakeListTopicsResponse({ pagination: fakePagination({ totalCount: 50, perPage: 10 }) }));

    const harness = await loader.getHarness(TopicsHarness);
    await harness.goToNextPage();

    httpTesting
      .expectOne(req => req.url === '/api/v2/kafka-explorer/list-topics' && req.params.get('page') === '2')
      .flush(fakeListTopicsResponse());
  });

  it('should navigate to topic detail on topic select', async () => {
    flushTopics();

    const harness = await loader.getHarness(TopicsHarness);
    const rows = await harness.getRows();
    await (await rows[0].host()).click();

    expect(router.navigate).toHaveBeenCalledWith(['my-topic'], { relativeTo: route });
  });
});
