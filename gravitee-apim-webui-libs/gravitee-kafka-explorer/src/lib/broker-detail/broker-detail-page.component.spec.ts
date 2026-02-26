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

import { BrokerDetailPageComponent } from './broker-detail-page.component';
import { BrokerDetailHarness } from './broker-detail.harness';
import { fakeDescribeBrokerResponse } from '../models/kafka-cluster.fixture';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';

describe('BrokerDetailPageComponent', () => {
  let fixture: ComponentFixture<BrokerDetailPageComponent>;
  let httpTesting: HttpTestingController;
  let loader: HarnessLoader;

  const router = { navigate: jest.fn() };
  const route = { snapshot: { params: { brokerId: '0' } } };

  beforeEach(async () => {
    router.navigate.mockClear();

    await TestBed.configureTestingModule({
      imports: [BrokerDetailPageComponent],
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

    fixture = TestBed.createComponent(BrokerDetailPageComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushBroker(response = fakeDescribeBrokerResponse()) {
    httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-broker').flush(response);
    fixture.detectChanges();
  }

  it('should call describeBroker on init with route param', () => {
    const req = httpTesting.expectOne(req => req.url === '/api/v2/kafka-explorer/describe-broker');
    expect(req.request.body).toEqual({ clusterId: 'test-cluster-id', brokerId: 0 });
    req.flush(fakeDescribeBrokerResponse());
  });

  it('should display broker id and host', async () => {
    flushBroker();

    const harness = await loader.getHarness(BrokerDetailHarness);
    expect(await harness.getBrokerTitle()).toBe('Broker 0');
  });

  it('should show controller badge for controller broker', async () => {
    flushBroker(fakeDescribeBrokerResponse({ isController: true }));

    const harness = await loader.getHarness(BrokerDetailHarness);
    expect(await harness.isController()).toBe(true);
  });

  it('should not show controller badge for non-controller broker', async () => {
    flushBroker(fakeDescribeBrokerResponse({ isController: false }));

    const harness = await loader.getHarness(BrokerDetailHarness);
    expect(await harness.isController()).toBe(false);
  });

  it('should display log directory entries table', async () => {
    flushBroker();

    const harness = await loader.getHarness(BrokerDetailHarness);
    const rows = await harness.getLogDirRows();
    expect(rows.length).toBe(1);
    expect(rows[0]['path']).toBe('/bitnami/kafka/data');
    expect(rows[0]['error']).toBe('—');
    expect(rows[0]['topics']).toBe('5');
    expect(rows[0]['partitions']).toBe('12');
    expect(rows[0]['size']).toBe('512 KB');
  });

  it('should display config table', async () => {
    flushBroker();

    const harness = await loader.getHarness(BrokerDetailHarness);
    const rows = await harness.getConfigRows();
    expect(rows.length).toBe(2);
    expect(rows[0]['name']).toBe('log.retention.hours');
    expect(rows[0]['value']).toBe('168');
    expect(rows[0]['source']).toBe('STATIC_BROKER_CONFIG');
  });

  it('should show empty message when no log dir entries', async () => {
    flushBroker(fakeDescribeBrokerResponse({ logDirEntries: [] }));

    const harness = await loader.getHarness(BrokerDetailHarness);
    expect(await harness.getLogDirEmptyMessage()).toBe('Log dir data not available');
    expect(await harness.getLogDirRows()).toEqual([]);
  });

  it('should show loading bar while loading', async () => {
    const harness = await loader.getHarness(BrokerDetailHarness);
    expect(await harness.isLoading()).toBe(true);

    flushBroker();
  });

  it('should navigate back on back button click', async () => {
    flushBroker();

    const harness = await loader.getHarness(BrokerDetailHarness);
    await harness.clickBack();

    expect(router.navigate).toHaveBeenCalledWith(['..'], { relativeTo: route });
  });
});
