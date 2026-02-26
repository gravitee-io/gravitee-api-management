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

import { ConsumerGroupDetailPageComponent } from './consumer-group-detail-page.component';
import { ConsumerGroupDetailHarness } from './consumer-group-detail.harness';
import { fakeConsumerGroupOffset, fakeDescribeConsumerGroupResponse } from '../models/kafka-cluster.fixture';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';

describe('ConsumerGroupDetailPageComponent', () => {
  let fixture: ComponentFixture<ConsumerGroupDetailPageComponent>;
  let httpTesting: HttpTestingController;
  let loader: HarnessLoader;

  const router = { navigate: jest.fn() };
  const route = { snapshot: { params: { groupId: 'my-group' } } };

  beforeEach(async () => {
    router.navigate.mockClear();

    await TestBed.configureTestingModule({
      imports: [ConsumerGroupDetailPageComponent],
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

    fixture = TestBed.createComponent(ConsumerGroupDetailPageComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushGroup(response = fakeDescribeConsumerGroupResponse()) {
    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-consumer-group').flush(response);
    fixture.detectChanges();
  }

  it('should call describeConsumerGroup on init with route param', () => {
    const req = httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-consumer-group');
    expect(req.request.body).toEqual({ clusterId: 'test-cluster-id', groupId: 'my-group' });
    req.flush(fakeDescribeConsumerGroupResponse());
  });

  it('should display group id', async () => {
    flushGroup();

    const harness = await loader.getHarness(ConsumerGroupDetailHarness);
    expect(await harness.getGroupId()).toBe('my-group');
  });

  it('should display state badge', async () => {
    flushGroup();

    const harness = await loader.getHarness(ConsumerGroupDetailHarness);
    expect(await harness.getState()).toBe('STABLE');
  });

  it('should display members table', async () => {
    flushGroup();

    const harness = await loader.getHarness(ConsumerGroupDetailHarness);
    const rows = await harness.getMembersRows();
    expect(rows.length).toBe(2);
    expect(rows[0]['memberId']).toBe('member-1');
    expect(rows[0]['clientId']).toBe('client-1');
    expect(rows[0]['host']).toBe('/127.0.0.1');
    expect(rows[0]['assignments']).toContain('my-topic');
  });

  it('should display offsets table', async () => {
    flushGroup();

    const harness = await loader.getHarness(ConsumerGroupDetailHarness);
    const rows = await harness.getOffsetsRows();
    expect(rows.length).toBe(2);
    expect(rows[0]['topic']).toBe('my-topic');
    expect(rows[0]['partition']).toBe('0');
    expect(rows[0]['committedOffset']).toBe('50');
    expect(rows[0]['endOffset']).toBe('100');
    expect(rows[0]['lag']).toContain('50');
  });

  it('should show lag warning for offsets with lag > 0', async () => {
    flushGroup(
      fakeDescribeConsumerGroupResponse({
        offsets: [fakeConsumerGroupOffset({ lag: 100 })],
      }),
    );

    const harness = await loader.getHarness(ConsumerGroupDetailHarness);
    const rows = await harness.getOffsetsRows();
    expect(rows[0]['lag']).toContain('behind');
  });

  it('should show loading bar while loading', async () => {
    const harness = await loader.getHarness(ConsumerGroupDetailHarness);
    expect(await harness.isLoading()).toBe(true);

    flushGroup();
  });

  it('should not show loading bar after loading completes', async () => {
    flushGroup();

    const harness = await loader.getHarness(ConsumerGroupDetailHarness);
    expect(await harness.isLoading()).toBe(false);
  });

  it('should navigate back on back button click', async () => {
    flushGroup();

    const harness = await loader.getHarness(ConsumerGroupDetailHarness);
    await harness.clickBack();

    expect(router.navigate).toHaveBeenCalledWith(['..'], { relativeTo: route });
  });

  it('should navigate to topic detail on topic link click', async () => {
    flushGroup();

    const harness = await loader.getHarness(ConsumerGroupDetailHarness);
    await harness.clickOffsetTopicLink(0);

    expect(router.navigate).toHaveBeenCalledWith(['../../topics', 'my-topic'], { relativeTo: route });
  });
});
