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

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { Scope } from '../entities/alert';
import { fakeAlertStatus } from '../entities/alerts/alertStatus.fixture';

describe('AlertService', () => {
  let httpTestingController: HttpTestingController;
  let alertService: AlertService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    alertService = TestBed.inject<AlertService>(AlertService);
  });

  describe('getStatus', () => {
    it('should call the API with PLATFORM scope', (done) => {
      const alertStatus = fakeAlertStatus();

      alertService.getStatus(Scope.ENVIRONMENT).subscribe((response) => {
        expect(response).toMatchObject(alertStatus);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/platform/alerts/status`);
      expect(req.request.method).toEqual('GET');

      req.flush(alertStatus);
    });

    it('should call the API with API scope and referenceId', (done) => {
      const alertStatus = fakeAlertStatus();

      alertService.getStatus(Scope.API, 'apiId').subscribe((response) => {
        expect(response).toMatchObject(alertStatus);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/apiId/alerts/status`);
      expect(req.request.method).toEqual('GET');

      req.flush(alertStatus);
    });

    it('should call the API with APPLICATION scope and referenceId', (done) => {
      const alertStatus = fakeAlertStatus();

      alertService.getStatus(Scope.APPLICATION, 'appId').subscribe((response) => {
        expect(response).toMatchObject(alertStatus);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/appId/alerts/status`);
      expect(req.request.method).toEqual('GET');

      req.flush(alertStatus);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
