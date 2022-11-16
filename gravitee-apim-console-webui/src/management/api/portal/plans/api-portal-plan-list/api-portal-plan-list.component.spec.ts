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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatButtonToggleHarness } from '@angular/material/button-toggle/testing';

import { ApiPortalPlanListComponent } from './api-portal-plan-list.component';

import { ApiPortalPlansModule } from '../api-portal-plans.module';
import { ApiPlan } from '../../../../../entities/api';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { fakePlan } from '../../../../../entities/plan/plan.fixture';

describe('ApiPortalPlanListComponent', () => {
  const API_ID = 'api#1';
  const fakeUiRouter = { go: jest.fn() };
  let fixture: ComponentFixture<ApiPortalPlanListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('plansTable tests', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [ApiPortalPlansModule, NoopAnimationsModule, GioHttpTestingModule],
        providers: [
          { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
          { provide: UIRouterState, useValue: fakeUiRouter },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ApiPortalPlanListComponent);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    it('should display an empty table', fakeAsync(async () => {
      await initComponent([]);

      const { headerCells, rowCells } = await computePlansTableCells();
      expect(headerCells).toEqual([
        {
          actions: '',
          name: 'Name',
          security: 'Security',
          status: 'Status',
          tags: 'Tags',
        },
      ]);
      expect(rowCells).toEqual([['There is no plan (yet).']]);
    }));

    it('should display a table with one row', fakeAsync(async () => {
      const plan = fakePlan({ tags: ['🙅', '🔑'] });
      await initComponent([plan]);

      const { headerCells, rowCells } = await computePlansTableCells();
      expect(headerCells).toEqual([
        {
          actions: '',
          name: 'Name',
          security: 'Security',
          status: 'Status',
          tags: 'Tags',
        },
      ]);
      expect(rowCells).toEqual([['Free Spaceshuttle', 'KEY_LESS', 'PUBLISHED', '🙅, 🔑', '']]);
    }));

    it('should search closed plan on click', fakeAsync(async () => {
      const goldPlan = fakePlan({ name: 'gold plan ⭐️' });
      await initComponent([goldPlan]);

      await loader.getHarness(MatButtonToggleHarness.with({ text: 'CLOSED' })).then((btn) => btn.toggle());

      const closedPlan = fakePlan({ name: 'closed plan 🚪', status: 'CLOSED' });
      expectApiPlansListRequest([closedPlan], 'CLOSED');

      const { rowCells } = await computePlansTableCells();
      expect(rowCells).toEqual([['closed plan 🚪', 'KEY_LESS', 'CLOSED', '', '']]);
    }));
  });

  async function initComponent(plans: ApiPlan[]) {
    expectApiPlansListRequest(plans);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  }

  async function computePlansTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  function expectApiPlansListRequest(plans: ApiPlan[] = [], status?: string, security?: string) {
    const url = `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans?${status ? `status=${status}` : 'status=PUBLISHED'}${
      security ? `security=${security}` : ''
    }`;
    httpTestingController.expectOne(url, 'GET').flush(plans);
    httpTestingController.verify();
  }
});
