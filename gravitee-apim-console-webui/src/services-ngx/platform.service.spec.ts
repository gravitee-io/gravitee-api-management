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

import { PlatformService } from './platform.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakePlatformLogsResponse } from '../entities/platform/platformLogs.fixture';
import { PlatformLogsResponse } from '../entities/platform/platformLogs';

describe('PlatformService', () => {
  let service: PlatformService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PlatformService);
  });

  describe('get platform logs', () => {
    it('should exist', () => {
      expect(service).toBeDefined();
    });

    xit('should call the API', done => {
      const platformLogsResponse: PlatformLogsResponse[] = [fakePlatformLogsResponse()];

      service
        .getPlatformV2Logs({
          from: 100,
          to: 100000,
          page: 1,
          size: 5,
          field: '@timestamp',
          order: false,
        })
        .subscribe(result => {
          expect(result).toEqual(platformLogsResponse);
          done();
        });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/platform/logs?page=1&size=5&from=100&to=100000&field=@timestamp&order=false`,
        })
        .flush(platformLogsResponse);
    });
  });
});
