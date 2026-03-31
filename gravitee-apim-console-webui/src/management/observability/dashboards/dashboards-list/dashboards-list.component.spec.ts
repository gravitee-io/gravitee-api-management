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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { DashboardsListComponent } from './dashboards-list.component';

import { Constants } from '../../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';

describe('DashboardsListComponent', () => {
  let component: DashboardsListComponent;
  let fixture: ComponentFixture<DashboardsListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardsListComponent, NoopAnimationsModule, GioTestingModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: Constants, useValue: CONSTANTS_TESTING }],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardsListComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    // detectChanges is intentionally NOT called here: the debounceTime(200) timer
    // must be created inside each fakeAsync test so that tick() can advance it.
  });

  afterEach(() => {
    // Absorb any asset requests (e.g. icon SVGs from MatIconRegistry) before verifying
    httpTestingController.match(req => !req.url.includes('/analytics/dashboards'));
    httpTestingController.verify();
  });

  it('should create', fakeAsync(() => {
    fixture.detectChanges(); // starts the observable chain inside the fakeAsync zone
    tick(200); // advances past debounceTime(200)
    fixture.detectChanges();
    httpTestingController.expectOne(req => req.url.includes('/analytics/dashboards')).flush({ data: [], pagination: { totalCount: 0 } });
    expect(component).toBeTruthy();
  }));

  it('should display dashboards', fakeAsync(async () => {
    const dashboards = Array.from({ length: 8 }, (_, i) => ({
      id: `id-${i}`,
      name: `Dashboard ${i}`,
      createdAt: '2024-01-01',
      lastModified: '2024-01-01',
      labels: {},
      widgets: [],
    }));

    fixture.detectChanges(); // starts the observable chain inside the fakeAsync zone
    tick(200); // advances past debounceTime(200)
    fixture.detectChanges();

    httpTestingController
      .expectOne(req => req.url.includes('/analytics/dashboards'))
      .flush({ data: dashboards, pagination: { totalCount: 8 } });

    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    expect(rows.length).toBe(8);
  }));
});
