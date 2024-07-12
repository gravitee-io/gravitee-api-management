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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatIconHarness, MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute } from '@angular/router';

import { ApiEventsComponent } from './api-events.component';
import { ApiEventsModule } from './api-events.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeEvent } from '../../../../entities/event/event.fixture';
import { Event } from '../../../../entities/event/event';

describe('ApiNgEventsComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiEventsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, ApiEventsModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
    }).compileComponents();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('events table tests', () => {
    beforeEach(async () => {
      await init();
    });
    it('should display an empty table', fakeAsync(async () => {
      await initComponent([]);

      const { headerCells, rowCells } = await computeEventsTableCells();
      expect(headerCells).toEqual([
        {
          icon: '',
          type: 'Type',
          createdAt: 'Created at',
          user: 'User',
        },
      ]);
      expect(rowCells).toEqual([['There is no event (yet).']]);
    }));

    it('should display a table with events', fakeAsync(async () => {
      const events = [
        fakeEvent({ type: 'STOP_API', created_at: new Date('2020-02-22T20:22:02'), user: { id: 'user_id', displayName: 'John DOE' } }),
        fakeEvent({ type: 'START_API', created_at: new Date('2020-02-02T20:22:02') }),
        fakeEvent({ type: 'PUBLISH_API', created_at: new Date('2020-02-02T20:20:02') }),
      ];
      await initComponent(events);

      const { headerCells, rowCells } = await computeEventsTableCells();
      expect(headerCells).toEqual([
        {
          icon: '',
          type: 'Type',
          createdAt: 'Created at',
          user: 'User',
        },
      ]);
      expect(rowCells).toEqual([
        ['', 'Stopped', 'Feb 22, 2020, 8:22:02 PM', 'John DOE'],
        ['', 'Started', 'Feb 2, 2020, 8:22:02 PM', 'unknown'],
        ['', 'Deployed', 'Feb 2, 2020, 8:20:02 PM', 'unknown'],
      ]);
      // Expect icons
      expect(await loader.getHarness(MatIconHarness.with({ selector: '.type__api-started' }))).toBeTruthy();
      expect(await loader.getHarness(MatIconHarness.with({ selector: '.type__api-stopped' }))).toBeTruthy();
      expect(await loader.getHarness(MatIconHarness.with({ selector: '.type__api-deployed' }))).toBeTruthy();

      // Expect background color
      expect(await loader.getHarness(MatRowHarness.with({ selector: '.row__api-started' }))).toBeTruthy();
      expect(await loader.getHarness(MatRowHarness.with({ selector: '.row__api-stopped' }))).toBeTruthy();
      expect(await loader.getHarness(MatRowHarness.with({ selector: '.row__api-deployed' }))).toBeTruthy();
    }));
  });

  async function initComponent(events: Event[]) {
    fixture = TestBed.createComponent(ApiEventsComponent);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    expectApiGetEvents(events);

    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();

    expect(fixture.componentInstance.eventsTableDS).toBeDefined();
    expect(fixture.componentInstance.eventsTableDS.length).toEqual(events.length);
  }

  async function computeEventsTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#eventsTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  function expectApiGetEvents(events: Event[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/events/search?page=0&size=100&type=START_API,STOP_API,PUBLISH_API`,
        method: 'GET',
      })
      .flush({
        content: events,
        pageNumber: 0,
        totalElements: events.length,
        pageElements: events.length,
      });
    fixture.detectChanges();
  }
});
