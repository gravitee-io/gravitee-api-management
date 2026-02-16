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
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { InstanceListComponent } from './instance-list.component';
import { InstanceListModule } from './instance-list.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { InstanceListItem } from '../../../entities/instance/instanceListItem';
import { fakeInstanceListItem } from '../../../entities/instance/instanceListItem.fixture';

describe('InstanceListComponent', () => {
  let fixture: ComponentFixture<InstanceListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, GioTestingModule, InstanceListModule],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(InstanceListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should display a message when no instance', fakeAsync(async () => {
    expectInstancesSearchRequest([]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#instancesTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));
    expect(rowCells).toHaveLength(0);

    const tableElement = await table.host();
    expect(await tableElement.text()).toContain('There are no Gateway instances (yet).');
  }));

  it('should display gateway instances', fakeAsync(async () => {
    expectInstancesSearchRequest([
      fakeInstanceListItem({
        hostname: 'GW 1',
        state: 'STARTED',
        ip: '192.168.1.48',
        port: '8082',
        last_heartbeat_at: 1700213066567,
        operating_system_name: 'Mac OS X',
        tags: ['products', 'stocks', '!international'],
        tenant: 'europe',
        version: '4.2.0-SNAPSHOT (build: 123213) revision#123123',
      }),
      fakeInstanceListItem({
        hostname: 'GW 2',
        state: 'STOPPED',
        ip: '10.10.10.10',
        port: '8082',
        last_heartbeat_at: 1700209517057,
        operating_system_name: 'Linux',
        version: '4.2.0-SNAPSHOT (build: 1234) revision#1234',
      }),
      fakeInstanceListItem({
        hostname: 'GW 3',
        state: 'UNKNOWN',
        ip: '10.0.0.1',
        port: '8082',
        last_heartbeat_at: 1700126699369,
      }),
    ]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#instancesTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByColumnName()));
    expect(rowCells).toStrictEqual([
      {
        hostname: 'GW 1',
        version: '4.2.0-SNAPSHOT',
        state: '',
        lastHeartbeat: 'Nov 17, 2023, 9:24:26 AM',
        os: 'Mac OS X',
        'ip-port': '192.168.1.48:8082',
        tags: 'products, stocks, !international',
        tenant: 'europe',
      },
      {
        hostname: 'GW 2',
        version: '4.2.0-SNAPSHOT',
        state: '',
        lastHeartbeat: 'Nov 17, 2023, 8:25:17 AM',
        os: 'Linux',
        'ip-port': '10.10.10.10:8082',
        tags: '',
        tenant: '',
      },
      {
        hostname: 'GW 3',
        version: '3.20.0-SNAPSHOT',
        state: '',
        lastHeartbeat: 'Nov 16, 2023, 9:24:59 AM',
        os: 'Linux',
        'ip-port': '10.0.0.1:8082',
        tags: '',
        tenant: '',
      },
    ]);
  }));

  it('should handle version parsing correctly', fakeAsync(async () => {
    expectInstancesSearchRequest([
      fakeInstanceListItem({
        hostname: 'GW noBuildInfo',
        state: 'STARTED',
        ip: '1.1.1.1',
        port: '8080',
        last_heartbeat_at: 1700213066567,
        operating_system_name: 'Linux',
        version: '5.0.0-SNAPSHOT', // no "(" → should remain the same
      }),
      fakeInstanceListItem({
        hostname: 'GW nullVersion',
        state: 'STARTED',
        ip: '2.2.2.2',
        port: '8080',
        last_heartbeat_at: 1700213066567,
        operating_system_name: 'Linux',
        version: null, // null → should be an empty string
      }),
    ]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#instancesTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByColumnName()));

    expect(rowCells).toStrictEqual([
      {
        hostname: 'GW noBuildInfo',
        version: '5.0.0-SNAPSHOT', // stays the same
        state: '',
        lastHeartbeat: 'Nov 17, 2023, 9:24:26 AM',
        os: 'Linux',
        'ip-port': '1.1.1.1:8080',
        tags: '',
        tenant: '',
      },
      {
        hostname: 'GW nullVersion',
        version: '', // empty string
        state: '',
        lastHeartbeat: 'Nov 17, 2023, 9:24:26 AM',
        os: 'Linux',
        'ip-port': '2.2.2.2:8080',
        tags: '',
        tenant: '',
      },
    ]);
  }));

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectInstancesSearchRequest(content: InstanceListItem[]) {
    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.baseURL}/instances/?includeStopped=true&from=0&to=0&page=0&size=10`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush({
      content: content,
      pageNumber: 0,
      pageElements: content.length,
      totalElements: content.length,
    });
  }
});
