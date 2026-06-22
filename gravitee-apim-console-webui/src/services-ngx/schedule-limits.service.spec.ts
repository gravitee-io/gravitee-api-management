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

import { ScheduleLimitsService } from './schedule-limits.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('ScheduleLimitsService', () => {
  let httpTestingController: HttpTestingController;
  let service: ScheduleLimitsService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [GioTestingModule] });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ScheduleLimitsService);
  });

  afterEach(() => httpTestingController.verify());

  it('should fetch and cache schedule limits', () => {
    const limits = { autoFetch: 300_000, dynamicProperties: 60_000, dictionary: 0, healthcheck: 1_000 };
    const received: (typeof limits)[] = [];

    service.limits$.subscribe(value => received.push(value));
    service.limits$.subscribe(value => received.push(value));

    const request = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/schedule-limits`);
    expect(request.request.method).toEqual('GET');
    request.flush(limits);

    expect(received).toEqual([limits, limits]);
  });
});
