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

import { BrokersComponent } from './brokers.component';
import { BrokersHarness } from './brokers.harness';
import { fakeBrokerDetail } from '../models/kafka-cluster.fixture';
import { BrokerDetail } from '../models/kafka-cluster.model';

@Component({
  standalone: true,
  imports: [BrokersComponent],
  template: `<gke-brokers [nodes]="nodes" [controllerId]="controllerId" />`,
})
class TestHostComponent {
  nodes: BrokerDetail[] = [];
  controllerId = -1;
}

describe('BrokersComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let harness: BrokersHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
  });

  it('should render broker nodes in the table with enriched columns', async () => {
    host.nodes = [
      fakeBrokerDetail({ id: 0, host: 'kafka-broker-0.example.com' }),
      fakeBrokerDetail({ id: 1, host: 'kafka-broker-1.example.com' }),
      fakeBrokerDetail({ id: 2, host: 'kafka-broker-2.example.com' }),
    ];
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BrokersHarness);

    const rows = await harness.getRowsData();
    expect(rows.length).toBe(3);
    expect(rows[0]).toEqual({
      id: '0',
      host: 'kafka-broker-0.example.com',
      port: '9092',
      rack: '-',
      leaderPartitions: '10',
      replicaPartitions: '20',
      logDirSize: '1 GB',
    });
  });

  it('should show controller badge for the controller node', async () => {
    host.nodes = [
      fakeBrokerDetail({ id: 0, host: 'kafka-broker-0.example.com' }),
      fakeBrokerDetail({ id: 1, host: 'kafka-broker-1.example.com' }),
    ];
    host.controllerId = 0;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BrokersHarness);

    const rows = await harness.getRowsData();
    expect(rows[0]['id']).toContain('Controller');
    expect(rows[1]['id']).not.toContain('Controller');
  });

  it('should display rack when available', async () => {
    host.nodes = [fakeBrokerDetail({ id: 0, rack: 'us-east-1a' })];
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BrokersHarness);

    const rows = await harness.getRowsData();
    expect(rows[0]['rack']).toBe('us-east-1a');
  });

  it('should display dash for null logDirSize', async () => {
    host.nodes = [fakeBrokerDetail({ id: 0, logDirSize: null })];
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BrokersHarness);

    const rows = await harness.getRowsData();
    expect(rows[0]['logDirSize']).toBe('-');
  });

  it('should render an empty table when no nodes', async () => {
    host.nodes = [];
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BrokersHarness);

    expect(await harness.getRowCount()).toBe(0);
  });
});
