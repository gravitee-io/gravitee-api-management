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

import { ApiAlertsService } from './api-alerts.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeAlertTriggerEntity } from '../entities/alerts/alertTriggerEntity.fixtures';

describe('ApiAlertsService', () => {
  let httpTestingController: HttpTestingController;
  let service: ApiAlertsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiAlertsService>(ApiAlertsService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list alerts for an API', () => {
    it('should get API alerts', (done) => {
      const apiId = 'test_api';
      const fakeAlerts = [fakeAlertTriggerEntity({ referenceId: apiId })];

      service.listAlerts(apiId, false).subscribe((response) => {
        expect(response).toMatchObject(fakeAlerts);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/alerts?event_counts=false`,
      });

      req.flush(fakeAlerts);
    });
  });
});
