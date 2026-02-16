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

import { AlertService } from './alert.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { Scope } from '../entities/alert';
import { fakeAlertStatus } from '../entities/alerts/alertStatus.fixture';
import { fakeAlertTriggerEntity, fakeNewAlertTriggerEntity } from '../entities/alerts/alertTriggerEntity.fixtures';

describe('AlertService', () => {
  let httpTestingController: HttpTestingController;
  let alertService: AlertService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    alertService = TestBed.inject<AlertService>(AlertService);
  });

  describe('getStatus', () => {
    it('should call the API with PLATFORM scope', done => {
      const alertStatus = fakeAlertStatus();

      alertService.getStatus(Scope.ENVIRONMENT).subscribe(response => {
        expect(response).toMatchObject(alertStatus);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/platform/alerts/status`);
      expect(req.request.method).toEqual('GET');

      req.flush(alertStatus);
    });

    it('should call the API with API scope and referenceId', done => {
      const alertStatus = fakeAlertStatus();

      alertService.getStatus(Scope.API, 'apiId').subscribe(response => {
        expect(response).toMatchObject(alertStatus);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/apiId/alerts/status`);
      expect(req.request.method).toEqual('GET');

      req.flush(alertStatus);
    });

    it('should call the API with APPLICATION scope and referenceId', done => {
      const alertStatus = fakeAlertStatus();

      alertService.getStatus(Scope.APPLICATION, 'appId').subscribe(response => {
        expect(response).toMatchObject(alertStatus);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/appId/alerts/status`);
      expect(req.request.method).toEqual('GET');

      req.flush(alertStatus);
    });
  });

  describe('list alerts for an API', () => {
    it('should get API alerts', done => {
      const apiId = 'test_api';
      const fakeAlerts = [fakeAlertTriggerEntity({ reference_id: apiId })];

      alertService.listAlerts(apiId, false).subscribe(response => {
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

  describe('create alert for an API', () => {
    it('should create API alert', done => {
      const apiId = 'test_api';
      const fakeNewAlerts = fakeNewAlertTriggerEntity();

      alertService.createAlert(apiId, fakeNewAlerts).subscribe(() => done());

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/alerts`,
      });
      expect(req.request.body).toEqual(fakeNewAlerts);
      req.flush({});
    });
  });

  describe('delete alert for an API', () => {
    it('should delete API alert', done => {
      const apiId = 'api_id';
      const alertId = 'alert_id';

      alertService.deleteAlert(apiId, alertId).subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/alerts/${alertId}`,
        })
        .flush(null);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
