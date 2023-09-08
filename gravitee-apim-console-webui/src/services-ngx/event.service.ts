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

import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { Event } from '../entities/event/event';

export interface EventPage {
  content: Event[];
  pageElements: number;
  pageNumber: number;
  totalElements: number;
}

@Injectable({
  providedIn: 'root',
})
export class EventService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  public findById(apiId: string, eventId: string): Observable<Event> {
    return this.http.get<Event>(`${this.constants.env.baseURL}/apis/${apiId}/events/${eventId}`);
  }

  public search(type: string, apis: string, query: string, from: number, to: number, page: number, size: number): Observable<EventPage> {
    return this.http.get<EventPage>(
      `${this.constants.env.baseURL}/platform/events?type=${type}&query=${query}&api_ids=${apis}&from=${from}&to=${to}&page=${page}&size=${size}`,
    );
  }
}
