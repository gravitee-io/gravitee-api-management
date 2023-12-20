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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { NewTicket } from '../entities/ticket/newTicket';
import { Ticket } from '../entities/ticket/ticket';
import { TicketSearchResult } from '../entities/ticket/ticketSearchResult';
import { TicketsParam } from '../entities/ticket/ticketsParam';

@Injectable({
  providedIn: 'root',
})
export class TicketService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  create(newTicket: NewTicket): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/platform/tickets`, newTicket);
  }

  getTicket(ticketId: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.constants.env.baseURL}/platform/tickets/${ticketId}`);
  }

  search(params: TicketsParam): Observable<TicketSearchResult> {
    let queryParams = new HttpParams();

    Object.entries(params).forEach(([key, value]) => {
      if (value) {
        queryParams = queryParams.append(key, value);
      }
    });

    return this.http.get<TicketSearchResult>(`${this.constants.env.baseURL}/platform/tickets`, { params: queryParams });
  }
}
