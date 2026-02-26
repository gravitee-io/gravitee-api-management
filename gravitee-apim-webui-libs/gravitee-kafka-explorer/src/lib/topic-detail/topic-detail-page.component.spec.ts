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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';

import { TopicDetailPageComponent } from './topic-detail-page.component';
import { TopicDetailHarness } from './topic-detail.harness';
import { fakeDescribeTopicResponse, fakeKafkaNode, fakeTopicPartitionDetail } from '../models/kafka-cluster.fixture';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';

describe('TopicDetailPageComponent', () => {
  let fixture: ComponentFixture<TopicDetailPageComponent>;
  let httpTesting: HttpTestingController;
  let loader: HarnessLoader;

  const router = { navigate: jest.fn() };
  const route = { snapshot: { params: { topicName: 'my-topic' } } };

  beforeEach(async () => {
    router.navigate.mockClear();

    await TestBed.configureTestingModule({
      imports: [TopicDetailPageComponent],
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

    fixture = TestBed.createComponent(TopicDetailPageComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushTopic(response = fakeDescribeTopicResponse()) {
    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-topic').flush(response);
    fixture.detectChanges();
  }

  it('should call describeTopic on init with route param', () => {
    const req = httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-topic');
    expect(req.request.body).toEqual({ clusterId: 'test-cluster-id', topicName: 'my-topic' });
    req.flush(fakeDescribeTopicResponse());
  });

  it('should display topic name', async () => {
    flushTopic();

    const harness = await loader.getHarness(TopicDetailHarness);
    expect(await harness.getTopicName()).toBe('my-topic');
  });

  it('should not show internal badge for non-internal topic', async () => {
    flushTopic();

    const harness = await loader.getHarness(TopicDetailHarness);
    expect(await harness.isInternal()).toBe(false);
  });

  it('should show internal badge for internal topic', async () => {
    flushTopic(fakeDescribeTopicResponse({ internal: true }));

    const harness = await loader.getHarness(TopicDetailHarness);
    expect(await harness.isInternal()).toBe(true);
  });

  it('should display partitions table', async () => {
    flushTopic();

    const harness = await loader.getHarness(TopicDetailHarness);
    const rows = await harness.getPartitionsRows();
    expect(rows.length).toBe(2);
    expect(rows[0]['id']).toBe('0');
    expect(rows[0]['leader']).toContain('kafka-broker-0.example.com');
    expect(rows[0]['replicas']).toBe('2');
    expect(rows[0]['isr']).toBe('2');
    expect(rows[0]['offline']).toBe('0');
  });

  it('should display config table', async () => {
    flushTopic();

    const harness = await loader.getHarness(TopicDetailHarness);
    const rows = await harness.getConfigRows();
    expect(rows.length).toBe(2);
    expect(rows[0]['name']).toBe('retention.ms');
    expect(rows[0]['value']).toBe('604800000');
    expect(rows[0]['source']).toBe('DYNAMIC_TOPIC_CONFIG');
  });

  it('should show loading bar while loading', async () => {
    const harness = await loader.getHarness(TopicDetailHarness);
    expect(await harness.isLoading()).toBe(true);

    flushTopic();
  });

  it('should display offline count when replicas are offline', async () => {
    flushTopic(
      fakeDescribeTopicResponse({
        partitions: [
          fakeTopicPartitionDetail({
            id: 0,
            replicas: [fakeKafkaNode({ id: 0 }), fakeKafkaNode({ id: 1 })],
            isr: [fakeKafkaNode({ id: 0 })],
            offline: [fakeKafkaNode({ id: 1 })],
          }),
        ],
      }),
    );

    const harness = await loader.getHarness(TopicDetailHarness);
    const rows = await harness.getPartitionsRows();
    expect(rows[0]['offline']).toBe('1');
  });

  it('should navigate back on back button click', async () => {
    flushTopic();

    const harness = await loader.getHarness(TopicDetailHarness);
    await harness.clickBack();

    expect(router.navigate).toHaveBeenCalledWith(['..'], { relativeTo: route });
  });
});
