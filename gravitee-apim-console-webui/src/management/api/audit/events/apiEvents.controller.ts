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
import * as _ from 'lodash';
import { StateService } from '@uirouter/core';

import { ApiService } from '../../../../services/api.service';

class ApiEventsController {
  private eventsTimeline: any;
  private eventPageSize = 100;
  private eventPage = 0;
  private eventTypes = 'START_API,STOP_API';

  /* @ngInject */
  constructor(private $state: StateService, private ApiService: ApiService) {
    this.eventsTimeline = [];
    this._loadEvents();
  }

  initTimeline(events) {
    _.forEach(events, (event) => {
      const eventTimelineType = this.getEventTypeTimeline(event.type);
      const eventTimeline = {
        event: event,
        badgeClass: eventTimelineType.badgeClass,
        badgeIconClass: eventTimelineType.icon,
        title: event.type,
        when: event.created_at,
        user: event.user,
      };
      this.eventsTimeline.push(eventTimeline);
    });
  }

  reloadEventsTimeline(events) {
    this.eventsTimeline = [];
    this.initTimeline(events);
  }

  getEventTypeTimeline(eventType): any {
    const eventTypeTimeline: any = {};
    switch (eventType) {
      case 'START_API':
        eventTypeTimeline.icon = 'av:play_arrow';
        eventTypeTimeline.badgeClass = 'info';
        break;
      case 'STOP_API':
        eventTypeTimeline.icon = 'av:stop';
        eventTypeTimeline.badgeClass = 'danger';
        break;
      default:
        eventTypeTimeline.icon = 'action:check_circle';
        eventTypeTimeline.badgeClass = 'info';
    }
    return eventTypeTimeline;
  }

  private _loadEvents() {
    this.ApiService.searchApiEvents(
      this.eventTypes,
      this.$state.params.apiId,
      undefined,
      undefined,
      this.eventPage,
      this.eventPageSize,
      false,
    ).then((response) => {
      const data = response.data;
      const events = [...data.content];
      this.initTimeline(events);

      if (data.totalElements > data.pageNumber * this.eventPageSize + data.pageElements) {
        this.eventPage++;
        this._loadEvents();
      }
    });
  }
}

export default ApiEventsController;
