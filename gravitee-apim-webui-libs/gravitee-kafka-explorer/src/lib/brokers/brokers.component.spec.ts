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
import { fakeKafkaNode } from '../models/kafka-cluster.fixture';
import { KafkaNode } from '../models/kafka-cluster.model';

@Component({
  standalone: true,
  imports: [BrokersComponent],
  template: `<gke-brokers [nodes]="nodes" />`,
})
class TestHostComponent {
  nodes: KafkaNode[] = [];
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

  it('should render broker nodes in the table', async () => {
    host.nodes = [
      fakeKafkaNode({ id: 0, host: 'kafka-broker-0.example.com' }),
      fakeKafkaNode({ id: 1, host: 'kafka-broker-1.example.com' }),
      fakeKafkaNode({ id: 2, host: 'kafka-broker-2.example.com' }),
    ];
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BrokersHarness);

    const rows = await harness.getRowsData();
    expect(rows.length).toBe(3);
    expect(rows[0]).toEqual({ id: '0', host: 'kafka-broker-0.example.com', port: '9092' });
    expect(rows[1]).toEqual({ id: '1', host: 'kafka-broker-1.example.com', port: '9092' });
  });

  it('should render an empty table when no nodes', async () => {
    host.nodes = [];
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BrokersHarness);

    expect(await harness.getRowCount()).toBe(0);
  });
});
