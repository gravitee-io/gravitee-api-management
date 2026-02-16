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

import { EventService } from './event.service';

import { fakeEvent } from '../entities/event/event.fixture';
import { GioTestingModule } from '../shared/testing';

describe('EventService', () => {
  let httpTestingController: HttpTestingController;
  let eventService: EventService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    eventService = TestBed.inject<EventService>(EventService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('findById', () => {
    it('should call the API', done => {
      const apiId = 'api#1';
      const eventId = 'event#1';
      const responseEvent = fakeEvent({ id: eventId });

      eventService.findById(apiId, eventId).subscribe(result => {
        expect(result).toMatchObject(responseEvent);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/apis/${apiId}/events/${eventId}`,
        })
        .flush(responseEvent);
    });
  });

  describe('search', () => {
    it('should call the API', done => {
      const eventId = 'event#1';
      const responseEvent = { content: [fakeEvent({ id: eventId })] };

      eventService.search('START_API,STOP_API,PUBLISH_API,UNPUBLISH_API', '', '', 1691505958670, 1694097958670, 1, 10).subscribe(result => {
        expect(result).toMatchObject(responseEvent);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/platform/events?type=START_API,STOP_API,PUBLISH_API,UNPUBLISH_API&query=&api_ids=&from=1691505958670&to=1694097958670&page=1&size=10`,
        })
        .flush(responseEvent);
    });
  });
});
