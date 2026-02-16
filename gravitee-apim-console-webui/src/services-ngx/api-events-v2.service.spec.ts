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

import { ApiEventsV2Service } from './api-events-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakePagedResult } from '../entities/management-api-v2';
import { fakeEvent } from '../entities/management-api-v2/event';

describe('ApiEventsV2Service', () => {
  let httpTestingController: HttpTestingController;
  let service: ApiEventsV2Service;
  const API_ID = 'api-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiEventsV2Service>(ApiEventsV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('searchApiEvents', () => {
    it.each<any | jest.DoneCallback>([
      { queryParams: {}, queryURL: '?page=1&perPage=10' },
      { queryParams: { page: 2, perPage: 12 }, queryURL: '?page=2&perPage=12' },
      { queryParams: { page: 2, perPage: 12, from: 222 }, queryURL: '?page=2&perPage=12&from=222' },
      { queryParams: { page: 2, perPage: 12, to: 333 }, queryURL: '?page=2&perPage=12&to=333' },
      { queryParams: { page: 2, perPage: 12, from: 222, to: 333 }, queryURL: '?page=2&perPage=12&from=222&to=333' },
      { queryParams: { from: 222, to: 333 }, queryURL: '?page=1&perPage=10&from=222&to=333' },
      { queryParams: { types: 'PUBLISH_API,START_API' }, queryURL: '?page=1&perPage=10&types=PUBLISH_API,START_API' },
    ])('call the service with: $queryParams should call API with: $queryURL', ({ queryParams, queryURL }: any, done: jest.DoneCallback) => {
      const fakeResponse = fakePagedResult([fakeEvent({ properties: { API: API_ID, USER: 'user-id' } })]);
      service.searchApiEvents(API_ID, queryParams).subscribe(apiPlansResponse => {
        expect(apiPlansResponse.data).toEqual(fakeResponse.data);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/events${queryURL}`,
        method: 'GET',
      });

      req.flush(fakeResponse);
    });
  });
});
