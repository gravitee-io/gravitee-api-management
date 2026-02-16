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
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';

import { TicketsComponent } from './tickets.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { TicketsModule } from '../tickets.module';
import { Ticket } from '../../../entities/ticket/ticket';
import { TicketSearchResult } from '../../../entities/ticket/ticketSearchResult';
import { fakeTicket } from '../../../entities/ticket/ticket.fixture';

describe('TicketsComponent', () => {
  let fixture: ComponentFixture<TicketsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, TicketsModule],
    });

    fixture = TestBed.createComponent(TicketsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  it('should display a message when no tickets', async () => {
    respondToTicketSearchRequest([]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#ticketsTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));
    expect(rowCells).toHaveLength(0);

    const tableElement = await table.host();
    expect(await tableElement.text()).toContain('There are no tickets (yet).');
  });

  it('should display tickets', async () => {
    const fakeTicket1 = fakeTicket({
      id: '84b2ab68-cf33-4e92-8697-f0a82d2fc527',
      subject: 'test subject',
      created_at: '2021-09-01T14:00:00.000Z',
    });
    const fakeTicket2 = fakeTicket({
      id: 'd155536b-8c69-4b07-b632-40acafa0f4ff',
      subject: 'Ticket 2',
      created_at: '2022-09-02T14:00:00.000Z',
      api: undefined,
      application: undefined,
    });
    respondToTicketSearchRequest([fakeTicket1, fakeTicket2]);

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#ticketsTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));
    expect(rowCells).toStrictEqual([
      ['Sep 1, 2021, 2:00:00 PM', fakeTicket1.api, fakeTicket1.application, fakeTicket1.subject],
      ['Sep 2, 2022, 2:00:00 PM', '', '', fakeTicket2.subject],
    ]);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function respondToTicketSearchRequest(tickets: Ticket[]) {
    const response: TicketSearchResult = {
      pageNumber: 1,
      pageElements: 10,
      content: tickets,
      totalElements: tickets.length,
    };

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/platform/tickets?page=1&size=10&order=-created_at`,
      })
      .flush(response);
  }
});
