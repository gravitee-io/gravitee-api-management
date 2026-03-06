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

import { MessagesBrowserHarness } from './messages/messages-browser.harness';
import { MessagesPageComponent } from './messages-page.component';
import { fakeBrowseMessagesResponse, fakeDescribeTopicResponse, fakeTopicPartitionDetail } from '../../models/kafka-cluster.fixture';
import { KafkaExplorerStore } from '../../services/kafka-explorer-store.service';

describe('MessagesPageComponent', () => {
  let fixture: ComponentFixture<MessagesPageComponent>;
  let httpTesting: HttpTestingController;
  let loader: HarnessLoader;

  const router = { navigate: jest.fn() };
  const route = { snapshot: { params: { topicName: 'my-topic' } } };

  beforeEach(async () => {
    router.navigate.mockClear();

    await TestBed.configureTestingModule({
      imports: [MessagesPageComponent],
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

    fixture = TestBed.createComponent(MessagesPageComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushInitialRequests(topicResponse = fakeDescribeTopicResponse(), messagesResponse = fakeBrowseMessagesResponse()) {
    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-topic').flush(topicResponse);
    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/browse-messages').flush(messagesResponse);
    fixture.detectChanges();
  }

  it('should call describeTopic and browseMessages on init', () => {
    const topicReq = httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-topic');
    expect(topicReq.request.body).toEqual({ clusterId: 'test-cluster-id', topicName: 'my-topic' });

    const messagesReq = httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/browse-messages');
    expect(messagesReq.request.body).toEqual({ clusterId: 'test-cluster-id', topicName: 'my-topic', offsetMode: 'NEWEST' });
    expect(messagesReq.request.params.get('limit')).toBe('50');

    topicReq.flush(fakeDescribeTopicResponse());
    messagesReq.flush(fakeBrowseMessagesResponse());
  });

  it('should display topic name in title', async () => {
    flushInitialRequests();

    const harness = await loader.getHarness(MessagesBrowserHarness);
    expect(await harness.getTopicName()).toContain('my-topic');
  });

  it('should display messages table', async () => {
    flushInitialRequests();

    const harness = await loader.getHarness(MessagesBrowserHarness);
    const rows = await harness.getMessagesRows();
    expect(rows.length).toBe(3);
    expect(rows[0]['partition']).toBeDefined();
    expect(rows[0]['offset']).toBeDefined();
  });

  it('should navigate back on back button click', async () => {
    flushInitialRequests();

    const harness = await loader.getHarness(MessagesBrowserHarness);
    await harness.clickBack();

    expect(router.navigate).toHaveBeenCalledWith(['..'], { relativeTo: route });
  });

  it('should fetch messages on fetch button click', async () => {
    flushInitialRequests();

    const harness = await loader.getHarness(MessagesBrowserHarness);
    await harness.clickFetch();

    const req = httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/browse-messages');
    expect(req.request.body.clusterId).toBe('test-cluster-id');
    expect(req.request.body.topicName).toBe('my-topic');
    req.flush(fakeBrowseMessagesResponse());
  });

  it('should show empty message when no messages', async () => {
    flushInitialRequests(fakeDescribeTopicResponse(), { data: [], totalFetched: 0 });

    const harness = await loader.getHarness(MessagesBrowserHarness);
    expect(await harness.hasEmptyMessage()).toBe(true);
  });

  it('should pass search options to browseMessages on search', async () => {
    flushInitialRequests();

    fixture.componentInstance.onSearch({
      partition: 1,
      offsetMode: 'OLDEST',
      offsetValue: 100,
      keyFilter: 'order',
      limit: 10,
    });

    const req = httpTesting.expectOne(r => r.url === '/api/v2/kafka-explorer/browse-messages');
    expect(req.request.body).toEqual({
      clusterId: 'test-cluster-id',
      topicName: 'my-topic',
      partition: 1,
      offsetMode: 'OLDEST',
      offsetValue: 100,
      keyFilter: 'order',
    });
    expect(req.request.params.get('limit')).toBe('10');
    req.flush(fakeBrowseMessagesResponse());
  });

  it('should pass valueFilter to browseMessages when provided', async () => {
    flushInitialRequests();

    fixture.componentInstance.onSearch({
      offsetMode: 'OLDEST',
      keyFilter: 'key-1',
      valueFilter: 'hello',
      limit: 50,
    });

    const req = httpTesting.expectOne(r => r.url === '/api/v2/kafka-explorer/browse-messages');
    expect(req.request.body).toEqual({
      clusterId: 'test-cluster-id',
      topicName: 'my-topic',
      offsetMode: 'OLDEST',
      keyFilter: 'key-1',
      valueFilter: 'hello',
    });
    expect(req.request.params.get('limit')).toBe('50');
    req.flush(fakeBrowseMessagesResponse());
  });

  it('should pass valueFilter to browseMessages when provided', async () => {
    flushInitialRequests();

    fixture.componentInstance.onSearch({
      offsetMode: 'OLDEST',
      keyFilter: 'key-1',
      valueFilter: 'hello',
      limit: 50,
    });

    const req = httpTesting.expectOne(r => r.url === '/api/v2/kafka-explorer/browse-messages');
    expect(req.request.body).toEqual({
      clusterId: 'test-cluster-id',
      topicName: 'my-topic',
      offsetMode: 'OLDEST',
      keyFilter: 'key-1',
      valueFilter: 'hello',
    });
    expect(req.request.params.get('limit')).toBe('50');
    req.flush(fakeBrowseMessagesResponse());
  });

  it('should set partitionCount after describeTopic response', async () => {
    flushInitialRequests(
      fakeDescribeTopicResponse({ partitions: [fakeTopicPartitionDetail({ id: 0 }), fakeTopicPartitionDetail({ id: 1 })] }),
    );

    expect(fixture.componentInstance.partitionCount()).toBe(2);
  });

  it('should set tailActive to true when onStartTail is called', () => {
    flushInitialRequests();

    fixture.componentInstance.onStartTail({
      partition: undefined,
      keyFilter: undefined,
      valueFilter: undefined,
      maxMessages: 1000,
      durationSeconds: 30,
    });

    expect(fixture.componentInstance.tailActive()).toBe(true);
    expect(fixture.componentInstance.messages()).toEqual([]);
    expect(fixture.componentInstance.totalFetched()).toBe(0);

    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/tail-messages');

    fixture.componentInstance.onStopTail();
  });

  it('should set tailActive to false when onStopTail is called', () => {
    flushInitialRequests();

    fixture.componentInstance.onStartTail({
      partition: undefined,
      keyFilter: undefined,
      valueFilter: undefined,
      maxMessages: 1000,
      durationSeconds: 30,
    });

    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/tail-messages');

    fixture.componentInstance.onStopTail();

    expect(fixture.componentInstance.tailActive()).toBe(false);
  });

  it('should clean up tail subscription on destroy', () => {
    flushInitialRequests();

    fixture.componentInstance.onStartTail({
      partition: undefined,
      keyFilter: undefined,
      valueFilter: undefined,
      maxMessages: 1000,
      durationSeconds: 30,
    });

    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/tail-messages');

    fixture.destroy();

    expect(fixture.componentInstance.tailActive()).toBe(false);
  });

  it('should send tail request with correct parameters', () => {
    flushInitialRequests();

    fixture.componentInstance.onStartTail({
      partition: 2,
      keyFilter: 'order',
      valueFilter: 'completed',
      maxMessages: 500,
      durationSeconds: 60,
    });

    const req = httpTesting.expectOne(r => r.url === '/api/v2/kafka-explorer/tail-messages');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('clusterId')).toBe('test-cluster-id');
    expect(req.request.params.get('topicName')).toBe('my-topic');
    expect(req.request.params.get('partition')).toBe('2');
    expect(req.request.params.get('keyFilter')).toBe('order');
    expect(req.request.params.get('valueFilter')).toBe('completed');
    expect(req.request.params.get('maxMessages')).toBe('500');
    expect(req.request.params.get('durationSeconds')).toBe('60');

    fixture.componentInstance.onStopTail();
  });
});
