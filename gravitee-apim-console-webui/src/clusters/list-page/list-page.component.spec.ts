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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ClustersListPageComponent } from './list-page.component';
import { ClustersListPageHarness } from './list-page.harness';

import { GioTestingModule } from '../../shared/testing';
import { GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';

describe('ClustersListPageComponent', () => {
  let fixture: ComponentFixture<ClustersListPageComponent>;
  let componentHarness: ClustersListPageHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClustersListPageComponent, NoopAnimationsModule, GioTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [],
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClustersListPageComponent);
    fixture.autoDetectChanges();
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ClustersListPageHarness);
  });

  it('should display clusters table', async () => {
    const table = await componentHarness.getTable();

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Production Cluster', 'kafka-prod.example.com:9092', 'SSL', ''],
      ['Development Cluster', 'kafka-dev.example.com:9092', 'SASL_PLAINTEXT', ''],
      ['Testing Cluster', 'kafka-test.example.com:9092', 'PLAINTEXT', ''],
      ['Staging Cluster', 'kafka-staging.example.com:9092', 'SASL_SSL', ''],
    ]);
  });

  it('should refresh the table when filters change', async () => {
    const table = await componentHarness.getTable();
    const getTableWrapper = await componentHarness.getTableWrapper();

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Production Cluster', 'kafka-prod.example.com:9092', 'SSL', ''],
      ['Development Cluster', 'kafka-dev.example.com:9092', 'SASL_PLAINTEXT', ''],
      ['Testing Cluster', 'kafka-test.example.com:9092', 'PLAINTEXT', ''],
      ['Staging Cluster', 'kafka-staging.example.com:9092', 'SASL_SSL', ''],
    ]);

    await getTableWrapper.setSearchValue('Production');

    expect(await table.getCellTextByIndex()).toStrictEqual([['Production Cluster', 'kafka-prod.example.com:9092', 'SSL', '']]);
  });
});
