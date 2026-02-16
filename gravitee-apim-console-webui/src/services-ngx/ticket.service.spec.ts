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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TicketService } from './ticket.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeNewTicket } from '../entities/ticket/newTicket.fixture';
import { fakeTicket } from '../entities/ticket/ticket.fixture';
import { TicketsParam } from '../entities/ticket/ticketsParam';
import { TicketSearchResult } from '../entities/ticket/ticketSearchResult';

describe('TicketService', () => {
  let httpTestingController: HttpTestingController;
  let ticketService: TicketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    ticketService = TestBed.inject<TicketService>(TicketService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create a ticket', done => {
    const newTicket = fakeNewTicket();

    ticketService.create(newTicket).subscribe(_ => {
      done();
    });

    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.baseURL}/platform/tickets`,
    });

    expect(req.request.body).toStrictEqual(newTicket);
    req.flush(null, { status: 201, statusText: 'Created' });
  });

  it('should get a ticket with its id', done => {
    const ticketId = '251ef36e-9680-4ed0-909b-fdda252b60a5';
    const ticket = fakeTicket({ id: ticketId });

    ticketService.getTicket(ticketId).subscribe(response => {
      expect(response).toStrictEqual(ticket);
      done();
    });

    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.baseURL}/platform/tickets/${ticketId}`,
    });

    req.flush(ticket);
  });

  it('should search tickets based on params', done => {
    const params: TicketsParam = {
      page: 1,
      size: 10,
      api: 'd07abcea-d0e4-48e5-88f3-cdaea936e90f',
      application: 'fc013e25-52d0-46b0-bcc8-fe237d18ee47',
      order: '-created_at',
    };
    const serverResponse: TicketSearchResult = {
      content: [fakeTicket({ id: '3b72ccd9-0a34-402b-848b-9c5e968d980f' }), fakeTicket({ id: 'd8729455-4fbe-4def-a384-fc5ff0c6fc55' })],
      totalElements: 2,
      pageElements: 2,
      pageNumber: 1,
    };

    ticketService.search(params).subscribe(response => {
      expect(response).toStrictEqual(serverResponse);
      done();
    });

    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.baseURL}/platform/tickets?page=${params.page}&size=${params.size}&api=${params.api}&application=${params.application}&order=${params.order}`,
    });

    req.flush(serverResponse);
  });
});
