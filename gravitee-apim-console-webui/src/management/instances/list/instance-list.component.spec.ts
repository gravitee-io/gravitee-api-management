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
import { InstanceListComponent } from './instance-list.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InstanceDetailsModule } from '../instance-details/instance-details.module';
import { fakeInstance } from '../../../entities/instance/instance.fixture';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { UIRouterModule } from '@uirouter/angular';
import { Instance } from '../../../entities/instance/instance';

describe('InstanceListComponent', () => {
  let fixture: ComponentFixture<InstanceListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        MatIconTestingModule,
        GioHttpTestingModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
        InstanceDetailsModule,
      ],
      providers: [{ provide: UIRouterStateParams, useValue: {} }],
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
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    expect(rowCells).toStrictEqual([['There are no Gateway instances (yet).']]);
  }));

  it('should display gateway instances', fakeAsync(async () => {
    expectInstancesSearchRequest([
      fakeInstance({
        hostname: 'GW 1',
        state: 'STARTED',
      }),
      fakeInstance({
        hostname: 'GW 2',
        state: 'STOPPED',
      }),
      fakeInstance({
        hostname: 'GW 3',
        state: 'UNKNOWN',
      }),
    ]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#instancesTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells).toStrictEqual([
      {
        hostname: 'GW 1',
        state: '',
      },
      {
        hostname: 'GW 2',
        state: '',
      },
      {
        hostname: 'GW 3',
        state: '',
      },
    ]);
  }));

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectInstancesSearchRequest(content: Instance[]) {
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
