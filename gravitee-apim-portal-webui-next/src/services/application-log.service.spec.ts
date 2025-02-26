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

describe('ApplicationLogService', () => {
  let service: ApplicationLogService;
  let httpTestingController: HttpTestingController;
  const APP_ID = 'app-id';

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AppTestingModule] });
    service = TestBed.inject(ApplicationLogService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe('search', () => {
    it('should return logs list with default parameters', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      service.search(APP_ID).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const currentDateInMilliseconds = Date.now();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=1&size=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        from: yesterdayInMilliseconds,
        to: currentDateInMilliseconds,
      });

      req.flush(logsResponse);
    });

    it('should return logs list with custom parameters', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const toDate = new Date(new Date().getDate() - 10);
      const fromDate = new Date(new Date().getDate() - 100);
      service.search(APP_ID, 2, 11, { to: toDate.getTime(), from: fromDate.getTime() }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=2&size=11`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        from: fromDate.getTime(),
        to: toDate.getTime(),
      });
      req.flush(logsResponse);
    });

    it('should return logs list with specified apis', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const currentDateInMilliseconds = Date.now();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const API_ID_1 = 'api-id-1';
      const API_ID_2 = 'api-id-2';
      service.search(APP_ID, 1, 10, { apiIds: [API_ID_1, API_ID_2] }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=1&size=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        from: yesterdayInMilliseconds,
        to: currentDateInMilliseconds,
        apiIds: [API_ID_1, API_ID_2],
      });
      req.flush(logsResponse);
    });

    it('should return logs list with specified methods', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const currentDateInMilliseconds = Date.now();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const GET_METHOD = 'GET';
      const POST_METHOD = 'POST';
      service.search(APP_ID, 1, 10, { methods: [GET_METHOD, POST_METHOD] }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=1&size=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        from: yesterdayInMilliseconds,
        to: currentDateInMilliseconds,
        methods: [GET_METHOD, POST_METHOD],
      });
      req.flush(logsResponse);
    });

    it('should return logs list with specified response times', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const currentDateInMilliseconds = Date.now();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const responseTimeOne = { from: 0, to: 100 };
      const responseTimeTwo = { from: 5000 };
      service.search(APP_ID, 1, 10, { responseTimeRanges: [responseTimeOne, responseTimeTwo] }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=1&size=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        from: yesterdayInMilliseconds,
        to: currentDateInMilliseconds,
        responseTimeRanges: [responseTimeOne, responseTimeTwo],
      });
      req.flush(logsResponse);
    });

    it('should return logs list with specified request id', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const currentDateInMilliseconds = Date.now();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const requestId = 'my-request';
      service.search(APP_ID, 1, 10, { requestId }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=1&size=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        from: yesterdayInMilliseconds,
        to: currentDateInMilliseconds,
        requestIds: [requestId],
      });
      req.flush(logsResponse);
    });

    it('should return logs list with specified transaction id', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const currentDateInMilliseconds = Date.now();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const transactionId = 'my-transaction';
      service.search(APP_ID, 1, 10, { transactionId }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=1&size=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        from: yesterdayInMilliseconds,
        to: currentDateInMilliseconds,
        transactionIds: [transactionId],
      });
      req.flush(logsResponse);
    });

    it('should return logs list with specified http statuses', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const currentDateInMilliseconds = Date.now();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const OK = '200';
      const NOT_FOUND = '404';
      service.search(APP_ID, 1, 10, { statuses: [OK, NOT_FOUND] }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=1&size=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        from: yesterdayInMilliseconds,
        to: currentDateInMilliseconds,
        statuses: [OK, NOT_FOUND],
      });
      req.flush(logsResponse);
    });

    // TODO: Fix test when backend can handle searching text in response body
    // it('should return logs list with specified message text', done => {
    //   const logsResponse: LogsResponse = fakeLogsResponse();
    //   const currentDateInMilliseconds = Date.now();
    //   const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
    //   const messageText = 'find me';
    //   service.search(APP_ID,1, 10, { messageText }).subscribe(response => {
    //     expect(response).toMatchObject(logsResponse);
    //     done();
    //   });
    //
    //   const req = httpTestingController.expectOne(
    //     `${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=1&size=10&order=DESC&field=@timestamp` +
    //       `&query=body:*${messageText}*`,
    //   );
    //   expect(req.request.method).toEqual('POST');
    //
    //   req.flush(logsResponse);
    // });

    it('should return logs list with specified path', done => {
      const logsResponse: LogsResponse = fakeLogsResponse();
      const currentDateInMilliseconds = Date.now();
      const yesterdayInMilliseconds = currentDateInMilliseconds - 86400000;
      const path = '/my/path';
      service.search(APP_ID, 1, 10, { path }).subscribe(response => {
        expect(response).toMatchObject(logsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/_search?page=1&size=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        from: yesterdayInMilliseconds,
        to: currentDateInMilliseconds,
        path,
      });
      req.flush(logsResponse);
    });
  });
});
