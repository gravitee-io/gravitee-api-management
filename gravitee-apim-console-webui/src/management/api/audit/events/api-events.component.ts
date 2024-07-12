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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../services-ngx/api.service';
import { Event, EventType } from '../../../../entities/event/event';
import { User } from '../../../../entities/user/user';

type EventsTableDS = {
  type: EventType;
  createdAt: Date;
  user: User;
};

@Component({
  selector: 'api-events',
  template: require('./api-events.component.html'),
  styles: [require('./api-events.component.scss')],
})
export class ApiEventsComponent implements OnInit, OnDestroy {
  private eventTypes = ['START_API', 'STOP_API', 'PUBLISH_API'];
  private eventPageSize = 100;
  private eventPage = 0;
  public displayedColumns = ['icon', 'type', 'createdAt', 'user'];
  public eventsTableDS: EventsTableDS[] = [];
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public isLoadingData = true;
  constructor(@Inject(UIRouterStateParams) private readonly ajsStateParams, private readonly apiService: ApiService) {}

  public ngOnInit(): void {
    this.isLoadingData = true;
    this._loadEvents();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private _loadEvents() {
    this.apiService
      .searchApiEvents(this.eventTypes, this.ajsStateParams.apiId, undefined, undefined, this.eventPage, this.eventPageSize, false)
      .pipe(
        tap((response) => {
          const events = [...response.content];
          this.eventsTableDS = [
            ...this.eventsTableDS,
            ...events.map((event: Event) => {
              return {
                type: event.type,
                createdAt: event.created_at,
                user: event.user,
              };
            }),
          ];

          if (response.totalElements > response.pageNumber * this.eventPageSize + response.pageElements) {
            this.eventPage++;
            this._loadEvents();
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => (this.isLoadingData = false));
  }
}
