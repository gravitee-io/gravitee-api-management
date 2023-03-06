import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { MessageService } from './message.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { HttpMessagePayload, TextMessagePayload } from '../entities/message/messagePayload';

describe('MessageService', () => {
  let httpTestingController: HttpTestingController;
  let service: MessageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<MessageService>(MessageService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('sendFromPortal', () => {
    it('should call the API for sending text message from portal', (done) => {
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
    it('should call the API for sending http message from portal', (done) => {
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
