/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { ApplicationLogService } from './application-log.service';
import { LogsResponse } from '../entities/log/log';
import { fakeLogsResponse } from '../entities/log/log.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

/* eslint-disable no-useless-escape */

describe('ApplicationLogService', () => {
  let service: ApplicationLogService;
  let httpTestingController: HttpTestingController;
  const MOCK_DATE = new Date(1466424490000);
  const APP_ID = 'app-id';

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AppTestingModule] });
    service = TestBed.inject(ApplicationLogService);
    httpTestingController = TestBed.inject(HttpTestingController);
    jest.useFakeTimers().setSystemTime(MOCK_DATE);
  });

  describe('list', () => {
    it('should return logs list with default parameters', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      service.list(APP_ID, {}).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const currentDateInMilliseconds = MOCK_DATE.getTime();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const req = httpTestingController.expectOne(
        `${TESTING_BASE_URL}/applications/${APP_ID}/logs?page=1&size=10&from=${yesterdayInMilliseconds}&to=${currentDateInMilliseconds}&order=DESC&field=@timestamp`,
      );
      expect(req.request.method).toEqual('GET');

      req.flush(logsResponse);
    });

    it('should return logs list with custom parameters', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const toDate = new Date(MOCK_DATE.getDate() - 10);
      const fromDate = new Date(MOCK_DATE.getDate() - 100);
      service
        .list(APP_ID, { page: 2, size: 11, to: toDate.getTime(), from: fromDate.getTime(), field: 'name', order: 'ASC' })
        .subscribe(response => {
          expect(response).toMatchObject(logsResponse);
          done();
        });

      const req = httpTestingController.expectOne(
        `${TESTING_BASE_URL}/applications/${APP_ID}/logs?page=2&size=11&from=${fromDate.getTime()}&to=${toDate.getTime()}&order=ASC&field=name`,
      );
      expect(req.request.method).toEqual('GET');

      req.flush(logsResponse);
    });

    it('should return logs list with specified apis', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const currentDateInMilliseconds = MOCK_DATE.getTime();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const API_ID_1 = 'api-id-1';
      const API_ID_2 = 'api-id-2';
      service.list(APP_ID, { apis: [API_ID_1, API_ID_2] }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(
        `${TESTING_BASE_URL}/applications/${APP_ID}/logs?page=1&size=10&from=${yesterdayInMilliseconds}&to=${currentDateInMilliseconds}&order=DESC&field=@timestamp` +
          `&query=(api:\\"${API_ID_1}\\" OR \\"${API_ID_2}\\")`,
      );
      expect(req.request.method).toEqual('GET');

      req.flush(logsResponse);
    });

    it('should return logs list with specified methods', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const currentDateInMilliseconds = MOCK_DATE.getTime();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const GET_METHOD = { value: '3', label: 'GET' };
      const POST_METHOD = { value: '7', label: 'POST' };
      service.list(APP_ID, { methods: [GET_METHOD, POST_METHOD] }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(
        `${TESTING_BASE_URL}/applications/${APP_ID}/logs?page=1&size=10&from=${yesterdayInMilliseconds}&to=${currentDateInMilliseconds}&order=DESC&field=@timestamp` +
          `&query=(method:\\"${GET_METHOD.value}\\" OR \\"${POST_METHOD.value}\\")`,
      );
      expect(req.request.method).toEqual('GET');

      req.flush(logsResponse);
    });
  });
});
