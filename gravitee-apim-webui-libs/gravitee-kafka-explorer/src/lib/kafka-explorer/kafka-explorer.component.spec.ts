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
import { provideRouter, Router, RouterOutlet } from '@angular/router';

import { KafkaExplorerHarness } from './kafka-explorer.harness';
import { KAFKA_EXPLORER_ROUTES } from './kafka-explorer.routes';
import { fakeDescribeClusterResponse, fakeDescribeTopicResponse, fakeListTopicsResponse } from '../models/kafka-cluster.fixture';
import { KAFKA_EXPLORER_BASE_URL } from '../services/kafka-explorer-config.token';

@Component({
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
class TestHostComponent {}

describe('KafkaExplorerComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let harness: KafkaExplorerHarness;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          {
            path: 'clusters/:clusterId',
            children: [
              {
                path: 'explorer',
                children: KAFKA_EXPLORER_ROUTES,
              },
            ],
          },
        ]),
        { provide: KAFKA_EXPLORER_BASE_URL, useValue: '/api/v2' },
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    await router.navigateByUrl('/clusters/test-cluster-id/explorer');
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
  });

  it('should display sidebar with Brokers and Topics links', async () => {
    flushCluster();

    const labels = await harness.getSidebarLabels();
    expect(labels).toEqual(['Brokers', 'Topics']);
  });

  it('should display brokers section by default', async () => {
    flushCluster();

    const brokersHarness = await harness.getBrokersHarness();
    expect(brokersHarness).toBeTruthy();
  });

  it('should navigate to Topics section', async () => {
    flushCluster();

    await harness.selectSection('Topics');
    fixture.detectChanges();
    await fixture.whenStable();

    flushTopics();

    const topicsHarness = await harness.getTopicsHarness();
    expect(topicsHarness).toBeTruthy();

    const brokersHarness = await harness.getBrokersHarness();
    expect(brokersHarness).toBeNull();
  });

  it('should navigate back to Brokers section', async () => {
    flushCluster();

    await harness.selectSection('Topics');
    fixture.detectChanges();
    await fixture.whenStable();
    flushTopics();

    await harness.selectSection('Brokers');
    fixture.detectChanges();
    await fixture.whenStable();

    const brokersHarness = await harness.getBrokersHarness();
    expect(brokersHarness).toBeTruthy();

    const topicsHarness = await harness.getTopicsHarness();
    expect(topicsHarness).toBeNull();
  });

  it('should navigate to topic detail when clicking a topic row', async () => {
    flushCluster();

    await harness.selectSection('Topics');
    fixture.detectChanges();
    await fixture.whenStable();
    flushTopics();

    const topicsHarness = await harness.getTopicsHarness();
    const rows = await topicsHarness!.getRows();
    await (await rows[0].host()).click();
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-topic').flush(fakeDescribeTopicResponse());
    fixture.detectChanges();

    const detailHarness = await harness.getTopicDetailHarness();
    expect(detailHarness).toBeTruthy();

    const topicsAfter = await harness.getTopicsHarness();
    expect(topicsAfter).toBeNull();
  });

  it('should navigate back to topics list from topic detail', async () => {
    flushCluster();

    await harness.selectSection('Topics');
    fixture.detectChanges();
    await fixture.whenStable();
    flushTopics();

    const topicsHarness = await harness.getTopicsHarness();
    const rows = await topicsHarness!.getRows();
    await (await rows[0].host()).click();
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-topic').flush(fakeDescribeTopicResponse());
    fixture.detectChanges();

    const detailHarness = await harness.getTopicDetailHarness();
    await detailHarness!.clickBack();
    fixture.detectChanges();
    await fixture.whenStable();

    flushTopics();

    const topicsAfter = await harness.getTopicsHarness();
    expect(topicsAfter).toBeTruthy();

    const detailAfter = await harness.getTopicDetailHarness();
    expect(detailAfter).toBeNull();
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

  it('should redirect to brokers URL by default', async () => {
    flushCluster();

    expect(router.url).toBe('/clusters/test-cluster-id/explorer/brokers');
  });
});
