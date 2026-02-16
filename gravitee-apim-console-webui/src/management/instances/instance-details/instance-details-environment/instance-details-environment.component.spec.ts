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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { formatDate } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { InstanceDetailsEnvironmentModule } from './instance-details-environment.module';
import { InstanceDetailsEnvironmentComponent } from './instance-details-environment.component';

import { fakeInstance } from '../../../../entities/instance/instance.fixture';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';

describe('InstanceDetailsEnvironmentComponent', () => {
  let fixture: ComponentFixture<InstanceDetailsEnvironmentComponent>;
  let loader: HarnessLoader;

  let httpTestingController: HttpTestingController;
  const instanceId = '5bc17c57-b350-460d-817c-57b350060db3';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, InstanceDetailsEnvironmentModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { instanceId } }, fragment: of('') } }],
    }).compileComponents();
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(InstanceDetailsEnvironmentComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should display information table', async () => {
    const fakeInstanceObject = fakeInstance();

    fixture.detectChanges();
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/instances/${instanceId}`,
      })
      .flush(fakeInstanceObject);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#informationTable' }));
    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByColumnName()));

    expect(headerCells).toEqual([
      {
        icon: '',
        type: 'Type',
        value: 'Value',
      },
    ]);
    expect(rowCells).toEqual([
      {
        icon: '',
        type: 'Hostname',
        value: 'apim-master-v3-apim3-gateway-6575b8ccf7-m4s6j',
      },
      {
        icon: '',
        type: 'IP',
        value: '0.0.0.0',
      },
      {
        icon: '',
        type: 'Port',
        value: '8082',
      },
      {
        icon: '',
        type: 'State',
        value: 'STARTED',
      },
      {
        icon: '',
        type: 'Version',
        value: '3.20.0-SNAPSHOT (build: 174998) revision#a67b37a366',
      },
      {
        icon: '',
        type: 'Started at',
        value: formatDate('1667812198374', 'medium', 'en-US'),
      },
      {
        icon: '',
        type: 'Last heartbeat at',
        value: formatDate('1667813521610', 'medium', 'en-US'),
      },
    ]);
  });

  it('should display plugins table', async () => {
    const fakeInstanceObject = fakeInstance({
      plugins: [
        {
          id: 'plugin-1',
          name: 'Plugin 1',
          version: '1.0.0',
          type: 'policy',
          description: 'Description 1',
          plugin: 'io.gravitee.plugin.policy.Plugin1',
        },
      ],
    });

    fixture.detectChanges();
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/instances/${instanceId}`,
      })
      .flush(fakeInstanceObject);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#pluginsTable' }));
    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByColumnName()));

    expect(headerCells).toEqual([
      {
        icon: '',
        id: 'ID',
        name: 'Name',
        version: 'Version',
      },
    ]);
    expect(rowCells).toEqual([
      {
        icon: '',
        id: 'plugin-1',
        name: 'Plugin 1',
        version: '1.0.0',
      },
    ]);
  });

  it('should display system properties table', async () => {
    const fakeInstanceObject = fakeInstance({
      systemProperties: {
        key1: 'value-1',
        key2: 'value-2',
      },
    });

    fixture.detectChanges();
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/instances/${instanceId}`,
      })
      .flush(fakeInstanceObject);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#propertiesTable' }));
    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByColumnName()));

    expect(headerCells).toEqual([
      {
        name: 'Name',
        value: 'Value',
      },
    ]);
    expect(rowCells).toEqual([
      {
        name: 'key1',
        value: 'value-1',
      },
      {
        name: 'key2',
        value: 'value-2',
      },
    ]);
  });
});
