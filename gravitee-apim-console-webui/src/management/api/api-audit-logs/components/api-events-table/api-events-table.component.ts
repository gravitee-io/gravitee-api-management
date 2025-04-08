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
import { Component, Input } from '@angular/core';

import { Event } from '../../../../../entities/management-api-v2/event';

@Component({
  selector: 'api-events-table',
  templateUrl: './api-events-table.component.html',
  styleUrls: ['./api-events-table.component.scss'],
  standalone: false,
})
export class ApiEventsTableComponent {
  protected displayedColumns = ['icon', 'type', 'createdAt', 'user'];
  private _events?: Event[];

  public get events(): Event[] | undefined {
    return this._events;
  }
  @Input()
  public set events(value: Event[] | undefined) {
    this._events = value;
  }
}
