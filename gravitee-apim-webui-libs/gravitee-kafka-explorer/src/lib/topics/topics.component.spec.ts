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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { TopicsComponent } from './topics.component';
import { TopicsHarness } from './topics.harness';
import { fakeKafkaTopic } from '../models/kafka-cluster.fixture';
import { KafkaTopic } from '../models/kafka-cluster.model';

@Component({
  standalone: true,
  imports: [TopicsComponent],
  template: `<gke-topics
    [topics]="topics"
    [totalElements]="totalElements"
    [page]="page"
    [pageSize]="pageSize"
    [loading]="loading"
    (filterChange)="lastFilter = $event"
    (pageChange)="lastPageEvent = $event"
  />`,
})
class TestHostComponent {
  topics: KafkaTopic[] = [];
  totalElements = 0;
  page = 0;
  pageSize = 25;
  loading = false;
  lastFilter: string | undefined;
  lastPageEvent: { page: number; pageSize: number } | undefined;
}

describe('TopicsComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let harness: TopicsHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
  });

  it('should render topics in the table', async () => {
    host.topics = [
      fakeKafkaTopic({ name: 'my-topic', partitionCount: 3, replicationFactor: 2, underReplicatedCount: 0, internal: false }),
      fakeKafkaTopic({ name: 'orders', partitionCount: 6, replicationFactor: 3, underReplicatedCount: 1, internal: false }),
    ];
    host.totalElements = 2;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    const rows = await harness.getRowsData();
    expect(rows.length).toBe(2);
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
    host.topics = [fakeKafkaTopic({ name: 'topic-1', underReplicatedCount: 2 })];
    host.totalElements = 1;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    const rows = await harness.getRowsData();
    expect(rows[0]['underReplicatedCount']).toContain('2');
  });

  it('should show Internal badge next to name for internal topics', async () => {
    host.topics = [fakeKafkaTopic({ name: '__consumer_offsets', internal: true })];
    host.totalElements = 1;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    const rows = await harness.getRowsData();
    expect(rows[0]['name']).toContain('__consumer_offsets');
    expect(rows[0]['name']).toContain('Internal');
  });

  it('should not show Internal badge for non-internal topics', async () => {
    host.topics = [fakeKafkaTopic({ name: 'my-topic', internal: false })];
    host.totalElements = 1;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    const rows = await harness.getRowsData();
    expect(rows[0]['name']).toBe('my-topic');
    expect(rows[0]['name']).not.toContain('Internal');
  });

  it('should emit filterChange when typing in filter', async () => {
    host.topics = [fakeKafkaTopic({ name: 'orders' })];
    host.totalElements = 1;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    await harness.setFilter('order');
    expect(host.lastFilter).toBe('order');
  });

  it('should render an empty table when no topics', async () => {
    host.topics = [];
    host.totalElements = 0;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    expect(await harness.getRowCount()).toBe(0);
  });

  it('should display size and messageCount columns', async () => {
    host.topics = [fakeKafkaTopic({ name: 'my-topic', size: 1048576, messageCount: 1500 })];
    host.totalElements = 1;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    const rows = await harness.getRowsData();
    expect(rows[0]['size']).toBe('1 MB');
    expect(rows[0]['messageCount']).toContain('1,500');
  });

  it('should display dash for null size and messageCount', async () => {
    host.topics = [fakeKafkaTopic({ name: 'my-topic', size: null, messageCount: null })];
    host.totalElements = 1;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    const rows = await harness.getRowsData();
    expect(rows[0]['size']).toBe('-');
    expect(rows[0]['messageCount']).toBe('-');
  });

  it('should show paginator with correct range', async () => {
    host.topics = [fakeKafkaTopic({ name: 'my-topic' })];
    host.totalElements = 50;
    host.page = 0;
    host.pageSize = 25;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    const rangeLabel = await harness.getRangeLabel();
    expect(rangeLabel).toContain('1');
    expect(rangeLabel).toContain('50');
  });

  it('should show progress bar when loading', async () => {
    host.topics = [];
    host.totalElements = 0;
    host.loading = true;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    expect(await harness.isLoading()).toBe(true);
  });

  it('should not show progress bar when not loading', async () => {
    host.topics = [fakeKafkaTopic({ name: 'my-topic' })];
    host.totalElements = 1;
    host.loading = false;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    expect(await harness.isLoading()).toBe(false);
  });

  it('should emit pageChange when navigating', async () => {
    host.topics = [fakeKafkaTopic({ name: 'my-topic' })];
    host.totalElements = 50;
    host.page = 0;
    host.pageSize = 25;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicsHarness);

    await harness.goToNextPage();
    expect(host.lastPageEvent).toEqual({ page: 1, pageSize: 25 });
  });
});
