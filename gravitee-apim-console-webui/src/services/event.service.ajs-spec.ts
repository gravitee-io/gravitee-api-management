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
import { IHttpBackendService } from 'angular';

import { EventService } from './event.service';

import { fakeEvent } from '../entities/event/event.fixture';
import { setupAngularJsTesting } from '../../old-jest.setup.js';

setupAngularJsTesting();

describe('EventService', () => {
  let eventService: EventService;
  let $httpBackend: IHttpBackendService;

  beforeEach(inject((_eventService_, _$httpBackend_) => {
    eventService = _eventService_;
    $httpBackend = _$httpBackend_;
  }));

  afterEach(() => {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('findById', () => {
    it('calls the endpoint', done => {
      const apiId = 'api#1';
      const eventId = 'event#1';
      const responseEvent = fakeEvent({ id: eventId });
      $httpBackend
        .expectGET(`https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/apis/${apiId}/events/${eventId}`)
        .respond(responseEvent);

      eventService
        .findById(apiId, eventId)
        .then(result => {
          expect(result).toEqual(responseEvent);
          done();
        })
        .catch(done.fail);

      $httpBackend.flush();
    });
  });
});
