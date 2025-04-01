/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';

import { RuntimeAlertListModule } from './runtime-alert-list.module';
import { RuntimeAlertListHarness } from './runtime-alert-list.harness';

import { AlertTriggerEntity } from '../../../entities/alerts/alertTriggerEntity';
import { fakeAlertTriggerEntity } from '../../../entities/alerts/alertTriggerEntity.fixtures';
import { GioTestingModule } from '../../../shared/testing';

@Component({
  selector: 'test-component',
  template: ` <runtime-alert-list [alerts]="alerts"></runtime-alert-list>`,
  standalone: false,
})
class TestComponent {
  alerts: AlertTriggerEntity[] = [
    fakeAlertTriggerEntity({ counters: { '5m': 1, '1h': 2, '1d': 3, '1M': 4 } }),
    fakeAlertTriggerEntity({ last_alert_at: undefined, last_alert_message: undefined }),
  ];
}

describe('RuntimeAlertListComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let runtimeAlertListHarness: RuntimeAlertListHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioTestingModule, RuntimeAlertListModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    runtimeAlertListHarness = await loader.getHarness(RuntimeAlertListHarness);
  });

  it('should display alerts table', async () => {
    const { headerCells, rowCells } = await runtimeAlertListHarness.computeTableCells();
    expect(headerCells).toStrictEqual([
      {
        actions: '',
        counters: 'Last 5m / 1h / 1d / 1M',
        description: 'description',
        lastAlert: 'Last alert',
        lastMessage: 'Last message',
        name: 'Name',
        severity: '',
      },
    ]);
    expect(rowCells).toHaveLength(2);
    expect(rowCells[0]).toStrictEqual(['my alert', 'INFO', 'description', '1 / 2 / 3 / 4', expect.anything(), 'last alert message', '']);
    expect(rowCells[1]).toStrictEqual(['my alert', 'INFO', 'description', '-', '-', '-', '']);
  });
});
