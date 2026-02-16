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
import { HttpTestingController } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { TicketComponent } from './ticket.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { TicketsModule } from '../tickets.module';
import { Ticket } from '../../../entities/ticket/ticket';
import { fakeTicket } from '../../../entities/ticket/ticket.fixture';

describe('TicketComponent', () => {
  const ticketId = '9a48a3b5-f459-4a06-851e-0834ec3ab47c';
  let httpTestingController: HttpTestingController;
  let fixture: ComponentFixture<TicketComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, TicketsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { ticketId },
            },
          },
        },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(TicketComponent);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create', () => {
    fixture.detectChanges();
    respondToGetTicket(fakeTicket({ id: ticketId }));
    fixture.detectChanges();

    const list = fixture.debugElement.query(By.css('.gio-description-list'));
    const displayedTexts: string[] = list.children.map(child => child.nativeElement.textContent.trim());
    expect(displayedTexts).toEqual([
      'API',
      '08cbff9a-1b1f-4430-836d-8849b0b23e7a',
      'Application',
      'da428f6f-b986-4a70-983b-55b83a9e7587',
      'Date',
      'Jan 1, 2020, 12:00:00 AM',
      'Subject',
      'A simple ticket',
      'Content',
      'Aper etiam atque etiam has exponere, ut quaeque',
    ]);
  });

  function respondToGetTicket(ticket: Ticket) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/platform/tickets/${ticket.id}`,
      })
      .flush(ticket);
  }
});
