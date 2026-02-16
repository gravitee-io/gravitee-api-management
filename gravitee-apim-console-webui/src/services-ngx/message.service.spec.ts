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

import { MessageService } from './message.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { HttpMessagePayload, TextMessagePayload } from '../entities/message/messagePayload';

describe('MessageService', () => {
  let httpTestingController: HttpTestingController;
  let service: MessageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<MessageService>(MessageService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('sendFromPortal', () => {
    it('should call the API for sending text message from portal', done => {
      const payload: TextMessagePayload = {
        channel: 'MAIL',
        recipient: { role_scope: 'APPLICATION', role_value: ['API', 'GROUP'] },
        text: 'Text',
        title: 'Title',
      };
      service.sendFromPortal(payload).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/messages`,
        method: 'POST',
      });

      expect(req.request.body).toEqual(payload);
      req.flush(1);
    });
  });

  describe('sendFromApi', () => {
    it('should call the API for sending http message from portal', done => {
      const httpMessagePayload: HttpMessagePayload = {
        channel: 'HTTP',
        recipient: { url: 'http://notification' },
        text: 'text',
        useSystemProxy: true,
        params: { param: 'value' },
      };
      const apiId = 'apiId';

      service.sendFromApi(apiId, httpMessagePayload).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/messages`,
        method: 'POST',
      });

      expect(req.request.body).toEqual(httpMessagePayload);
      req.flush(1);
    });
  });
});
