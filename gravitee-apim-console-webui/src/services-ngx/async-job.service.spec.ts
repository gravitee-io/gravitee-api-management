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

import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { AsyncJobService } from './async-job.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakePaginatedResult } from '../entities/paginatedResult';

describe('AsyncJobService', () => {
  const url = `${CONSTANTS_TESTING.env.v2BaseURL}/async-jobs`;
  let service: AsyncJobService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });
    service = TestBed.inject(AsyncJobService);

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<AsyncJobService>(AsyncJobService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('listAsyncJobs', () => {
    it('should call API', (done) => {
      const fakeData = fakePaginatedResult([]);

      service
        .listAsyncJobs(
          {
            type: 'SCORING_REQUEST',
            status: 'PENDING',
            sourceId: 'sourceId',
          },
          2,
          20,
        )
        .subscribe((res) => {
          expect(res).toMatchObject(fakeData);
          done();
        });

      httpTestingController
        .expectOne({ method: 'GET', url: url + '?page=2&perPage=20&type=SCORING_REQUEST&status=PENDING&sourceId=sourceId' })
        .flush(fakeData);
    });
  });
});
