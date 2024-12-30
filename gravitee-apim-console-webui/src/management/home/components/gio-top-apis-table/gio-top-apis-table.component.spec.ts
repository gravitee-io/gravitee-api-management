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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { SimpleChange } from '@angular/core';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { GioTopApisTableModule } from './gio-top-apis-table.module';
import { GioTopApisTableComponent, TopApisData } from './gio-top-apis-table.component';

describe('GioStatsTableComponent', () => {
  const data: TopApisData = {
    values: {
      '?': 2764281,
      '78416fd0-25ac-4234-816f-d025aca2345c': 351475,
      '62b7d292-8ee1-3913-8030-c883e01de8a0': 19,
      '5baa3ce2-5c8a-4a53-aa3c-e25c8a0a53aa': 17,
      '9cbd6331-fdc7-4362-bd63-31fdc71362ae': 2,
      'e78f07d4-d6d0-384e-8f8d-8cbe736074ad': 2,
      'b264ff24-9030-31ae-be7b-cd50e0a88920': 1,
    },
    metadata: {
      '78416fd0-25ac-4234-816f-d025aca2345c': {
        name: 'Snowcamp',
        version: '1',
        order: '1',
      },
      '5baa3ce2-5c8a-4a53-aa3c-e25c8a0a53aa': {
        name: 'Docs - APIM',
        version: '1.0',
        order: '3',
      },
      'e78f07d4-d6d0-384e-8f8d-8cbe736074ad': {
        name: 'API 1 with slow backend',
        version: '1',
        order: '5',
      },
      '62b7d292-8ee1-3913-8030-c883e01de8a0': {
        name: '4790',
        version: '1',
        order: '2',
      },
      '9cbd6331-fdc7-4362-bd63-31fdc71362ae': {
        name: 'test-bad-ssl',
        version: 'test-bad-ssl',
        order: '4',
      },
      'b264ff24-9030-31ae-be7b-cd50e0a88920': {
        name: 'API 2, call API 1 with slow backend',
        version: '1',
        order: '6',
      },
      '?': {
        name: 'Unknown API (not found)',
        unknown: true,
        order: '0',
      },
    },
  };

  let fixture: ComponentFixture<GioTopApisTableComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTopApisTableModule, RouterTestingModule],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GioTopApisTableComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();

    fixture.componentInstance.data = data;
    fixture.detectChanges();
    fixture.componentInstance.ngOnChanges({ data: new SimpleChange(null, data, true) });
  });

  it('should init', async () => {
    expect(loader).toBeTruthy();
    const tableHarness = await loader.getHarness(MatTableHarness);
    const rows = await tableHarness.getRows();
    expect(rows.length).toEqual(5);

    // First row must contain name, and no link
    const firstApi = await rows[0].getCellTextByIndex({ columnName: 'name' });
    expect(firstApi).toEqual(['Unknown API (not found)']);
    const cells = await rows[0].getCells();
    const link = await cells[0].getAllChildLoaders('a');
    expect(link.length).toEqual(0);

    // Second row must contain name, and a link
    const secondApi = await rows[1].getCellTextByIndex({ columnName: 'name' });
    expect(secondApi).toEqual(['Snowcamp']);
    const cells2 = await rows[1].getCells();
    const link2 = await cells2[0].getAllChildLoaders('a');
    expect(link2.length).toEqual(1);

    const nbHits = await rows[0].getCellTextByIndex({ columnName: 'value' });
    expect(nbHits).toEqual(['2,764,281']);

    const paginotorHarness = await loader.getHarness(MatPaginatorHarness);
    expect(paginotorHarness).toBeDefined();
  });
});
