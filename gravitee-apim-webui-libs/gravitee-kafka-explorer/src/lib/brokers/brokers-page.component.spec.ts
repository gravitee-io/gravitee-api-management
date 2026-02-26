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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';

import { BrokersPageComponent } from './brokers-page.component';
import { BrokersHarness } from './brokers.harness';
import { fakeBrokerDetail, fakeDescribeClusterResponse } from '../models/kafka-cluster.fixture';
import { KafkaExplorerStore } from '../services/kafka-explorer-store.service';

describe('BrokersPageComponent', () => {
  let fixture: ComponentFixture<BrokersPageComponent>;
  let store: KafkaExplorerStore;
  let loader: HarnessLoader;

  const router = { navigate: jest.fn() };
  const route = {};

  beforeEach(async () => {
    router.navigate.mockClear();

    await TestBed.configureTestingModule({
      imports: [BrokersPageComponent],
      providers: [
        KafkaExplorerStore,
        provideNoopAnimations(),
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: route },
      ],
    }).compileComponents();

    store = TestBed.inject(KafkaExplorerStore);
    fixture = TestBed.createComponent(BrokersPageComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should render broker nodes in the table with enriched columns', async () => {
    store.clusterInfo.set(fakeDescribeClusterResponse());
    fixture.detectChanges();

    const harness = await loader.getHarness(BrokersHarness);
    const rows = await harness.getRowsData();
    expect(rows.length).toBe(3);
    expect(rows[0]).toEqual({
      id: '0 Controller',
      host: 'kafka-broker-0.example.com',
      port: '9092',
      rack: '-',
      leaderPartitions: '10',
      replicaPartitions: '20',
      logDirSize: '1 GB',
    });
  });

  it('should show controller badge for the controller node', async () => {
    store.clusterInfo.set(
      fakeDescribeClusterResponse({
        nodes: [
          fakeBrokerDetail({ id: 0, host: 'kafka-broker-0.example.com' }),
          fakeBrokerDetail({ id: 1, host: 'kafka-broker-1.example.com' }),
        ],
      }),
    );
    fixture.detectChanges();

    const harness = await loader.getHarness(BrokersHarness);
    const rows = await harness.getRowsData();
    expect(rows[0]['id']).toContain('Controller');
    expect(rows[1]['id']).not.toContain('Controller');
  });

  it('should display rack when available', async () => {
    store.clusterInfo.set(
      fakeDescribeClusterResponse({
        nodes: [fakeBrokerDetail({ id: 0, rack: 'us-east-1a' })],
      }),
    );
    fixture.detectChanges();

    const harness = await loader.getHarness(BrokersHarness);
    const rows = await harness.getRowsData();
    expect(rows[0]['rack']).toBe('us-east-1a');
  });

  it('should display dash for null logDirSize', async () => {
    store.clusterInfo.set(
      fakeDescribeClusterResponse({
        nodes: [fakeBrokerDetail({ id: 0, logDirSize: null })],
      }),
    );
    fixture.detectChanges();

    const harness = await loader.getHarness(BrokersHarness);
    const rows = await harness.getRowsData();
    expect(rows[0]['logDirSize']).toBe('-');
  });

  it('should not render when cluster info is not available', async () => {
    fixture.detectChanges();

    const harness = await loader.getHarnessOrNull(BrokersHarness);
    expect(harness).toBeNull();
  });

  it('should navigate to broker detail on broker select', async () => {
    store.clusterInfo.set(fakeDescribeClusterResponse());
    fixture.detectChanges();

    const harness = await loader.getHarness(BrokersHarness);
    const rows = await harness.getRows();
    await rows[0].host().then(host => host.click());

    expect(router.navigate).toHaveBeenCalledWith([0], { relativeTo: route });
  });
});
