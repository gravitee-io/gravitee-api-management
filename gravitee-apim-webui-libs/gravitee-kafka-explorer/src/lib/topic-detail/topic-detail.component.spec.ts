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
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { TopicDetailComponent } from './topic-detail.component';
import { TopicDetailHarness } from './topic-detail.harness';
import { fakeDescribeTopicResponse, fakeKafkaNode, fakeTopicPartitionDetail } from '../models/kafka-cluster.fixture';
import { DescribeTopicResponse } from '../models/kafka-cluster.model';

@Component({
  standalone: true,
  imports: [TopicDetailComponent],
  template: `<gke-topic-detail [topicDetail]="topicDetail()" [loading]="loading()" (back)="backCalled = true" />`,
})
class TestHostComponent {
  topicDetail = signal<DescribeTopicResponse | undefined>(undefined);
  loading = signal(false);
  backCalled = false;
}

describe('TopicDetailComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let harness: TopicDetailHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.topicDetail.set(fakeDescribeTopicResponse());
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopicDetailHarness);
  });

  it('should display topic name', async () => {
    expect(await harness.getTopicName()).toBe('my-topic');
  });

  it('should not show internal badge for non-internal topic', async () => {
    expect(await harness.isInternal()).toBe(false);
  });

  it('should show internal badge for internal topic', async () => {
    fixture.componentInstance.topicDetail.set(fakeDescribeTopicResponse({ internal: true }));
    fixture.detectChanges();

    expect(await harness.isInternal()).toBe(true);
  });

  it('should display partitions table', async () => {
    const rows = await harness.getPartitionsRows();
    expect(rows.length).toBe(2);
    expect(rows[0]['id']).toBe('0');
    expect(rows[0]['leader']).toContain('kafka-broker-0.example.com');
    expect(rows[0]['replicas']).toBe('2');
    expect(rows[0]['isr']).toBe('2');
    expect(rows[0]['offline']).toBe('0');
  });

  it('should display config table', async () => {
    const rows = await harness.getConfigRows();
    expect(rows.length).toBe(2);
    expect(rows[0]['name']).toBe('retention.ms');
    expect(rows[0]['value']).toBe('604800000');
    expect(rows[0]['source']).toBe('DYNAMIC_TOPIC_CONFIG');
  });

  it('should emit back on back button click', async () => {
    await harness.clickBack();
    expect(fixture.componentInstance.backCalled).toBe(true);
  });

  it('should show loading bar when loading', async () => {
    fixture.componentInstance.loading.set(true);
    fixture.detectChanges();

    expect(await harness.isLoading()).toBe(true);
  });

  it('should display offline count when replicas are offline', async () => {
    fixture.componentInstance.topicDetail.set(
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
    fixture.detectChanges();

    const rows = await harness.getPartitionsRows();
    expect(rows[0]['offline']).toBe('1');
  });
});
