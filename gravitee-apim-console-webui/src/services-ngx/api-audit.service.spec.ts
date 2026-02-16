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

import { ApiAuditService } from './api-audit.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeMetadataPageAudit } from '../entities/audit/Audit.fixture';

describe('ApiAuditService', () => {
  let httpTestingController: HttpTestingController;
  let apiAuditService: ApiAuditService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiAuditService = TestBed.inject<ApiAuditService>(ApiAuditService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get audit', () => {
    it('should call the API', done => {
      const apiId = 'test_api';
      const fakeAuditPage = fakeMetadataPageAudit();

      apiAuditService.getAudit(apiId, {}).subscribe(response => {
        expect(response).toMatchObject(fakeAuditPage);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/audit?page=1&size=10`,
      });

      req.flush(fakeAuditPage);
    });
  });

  describe('get events', () => {
    it('should call the API', done => {
      const apiId = 'test_api';
      const fakeEvents = ['TESTEVENT1', 'TESTEVENT2'];

      apiAuditService.getEvents(apiId).subscribe(response => {
        expect(response).toMatchObject(fakeEvents);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/audit/events`,
      });

      req.flush(fakeEvents);
    });
  });
});
