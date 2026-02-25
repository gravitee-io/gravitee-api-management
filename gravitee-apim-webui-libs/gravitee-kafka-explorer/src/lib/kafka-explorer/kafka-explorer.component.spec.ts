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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { KafkaExplorerComponent } from './kafka-explorer.component';
import { KafkaExplorerHarness } from './kafka-explorer.harness';
import { fakeDescribeClusterResponse, fakeListTopicsResponse } from '../models/kafka-cluster.fixture';

@Component({
  standalone: true,
  imports: [KafkaExplorerComponent],
  template: `<gke-kafka-explorer [baseURL]="baseURL" [clusterId]="clusterId" />`,
})
class TestHostComponent {
  baseURL = '/api/v2';
  clusterId = 'test-cluster-id';
}

describe('KafkaExplorerComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let harness: KafkaExplorerHarness;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideNoopAnimations(), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, KafkaExplorerHarness);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushCluster() {
    httpTesting.expectOne('/api/v2/kafka-explorer/describe-cluster').flush(fakeDescribeClusterResponse());
    fixture.detectChanges();
  }

  function flushTopics() {
    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/list-topics').flush(fakeListTopicsResponse());
    fixture.detectChanges();
  }

  it('should show loader while loading', async () => {
    expect(await harness.isLoading()).toBe(true);

    flushCluster();
    flushTopics();
  });

  it('should display sidebar with Brokers and Topics buttons', async () => {
    flushCluster();
    flushTopics();

    const labels = await harness.getSidebarLabels();
    expect(labels).toEqual(['Brokers', 'Topics']);
  });

  it('should display cluster info in brokers section by default', async () => {
    const clusterReq = httpTesting.expectOne('/api/v2/kafka-explorer/describe-cluster');
    expect(clusterReq.request.body).toEqual({ clusterId: 'test-cluster-id' });
    clusterReq.flush(fakeDescribeClusterResponse());

    fixture.detectChanges();
    flushTopics();

    const brokersHarness = await harness.getBrokersHarness();
    expect(brokersHarness).toBeTruthy();

    const clusterInfoText = await brokersHarness!.getClusterInfoText();
    expect(clusterInfoText).toContain('test-cluster-id');
    expect(clusterInfoText).toContain('kafka-broker-0.example.com');
    expect(clusterInfoText).toContain('5');
    expect(clusterInfoText).toContain('15');
  });

  it('should render broker nodes table via BrokersHarness', async () => {
    flushCluster();
    flushTopics();

    const brokersHarness = await harness.getBrokersHarness();
    expect(brokersHarness).toBeTruthy();

    const rows = await brokersHarness!.getRowsData();
    expect(rows.length).toBe(3);
    expect(rows[0]['host']).toBe('kafka-broker-0.example.com');
    expect(rows[0]['port']).toBe('9092');
    expect(rows[0]['id']).toContain('0');
  });

  it('should navigate to Topics section', async () => {
    flushCluster();
    flushTopics();

    await harness.selectSection('Topics');
    fixture.detectChanges();

    const topicsHarness = await harness.getTopicsHarness();
    expect(topicsHarness).toBeTruthy();

    const rows = await topicsHarness!.getRowsData();
    expect(rows.length).toBe(3);
    expect(rows[0]['name']).toBe('my-topic');

    const brokersHarness = await harness.getBrokersHarness();
    expect(brokersHarness).toBeNull();
  });

  it('should navigate back to Brokers section', async () => {
    flushCluster();
    flushTopics();

    await harness.selectSection('Topics');
    fixture.detectChanges();

    await harness.selectSection('Brokers');
    fixture.detectChanges();

    const brokersHarness = await harness.getBrokersHarness();
    expect(brokersHarness).toBeTruthy();

    const topicsHarness = await harness.getTopicsHarness();
    expect(topicsHarness).toBeNull();
  });

  it('should show error on failure', async () => {
    httpTesting
      .expectOne('/api/v2/kafka-explorer/describe-cluster')
      .flush({ message: 'Connection refused' }, { status: 500, statusText: 'Error' });
    fixture.detectChanges();

    expect(await harness.isLoading()).toBe(false);
    const errorMessage = await harness.getErrorMessage();
    expect(errorMessage).toContain('Connection refused');
  });
});
