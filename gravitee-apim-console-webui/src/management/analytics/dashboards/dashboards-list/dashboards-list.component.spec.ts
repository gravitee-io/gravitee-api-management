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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatSortHarness } from '@angular/material/sort/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DashboardsListComponent } from './dashboards-list.component';

import { Constants } from '../../../../entities/Constants';
import { CONSTANTS_TESTING } from '../../../../shared/testing';

describe('DashboardsListComponent', () => {
  let component: DashboardsListComponent;
  let fixture: ComponentFixture<DashboardsListComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardsListComponent, NoopAnimationsModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: Constants, useValue: CONSTANTS_TESTING }],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display dashboards', async () => {
    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    // Default pagination is 10, and we have 8 items
    expect(rows.length).toBe(8);
  });

  it('should sort dashboards', async () => {
    const sort = await loader.getHarness(MatSortHarness);
    const headers = await sort.getSortHeaders({ sortDirection: '' });

    // Sort by name asc
    await headers[0].click();
    expect(await headers[0].getSortDirection()).toBe('asc');

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    const cells = await rows[0].getCells();
    // "AI Dashboard" starts with A and I comes before d, should be first
    expect(await cells[0].getText()).toBe('AI Dashboard');

    // Sort by name desc
    await headers[0].click();
    expect(await headers[0].getSortDirection()).toBe('desc');

    const rowsDesc = await table.getRows();
    const cellsDesc = await rowsDesc[0].getCells();
    // "V4 Proxy Dashboard" starts with V, should be first (or similar)
    expect(await cellsDesc[0].getText()).toBe('V4 Proxy Dashboard');
  });
});
