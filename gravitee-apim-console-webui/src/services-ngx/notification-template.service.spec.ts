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

import { NotificationTemplateService } from './notification-template.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeNotificationTemplate } from '../entities/notification/notificationTemplate.fixture';

describe('NotificationTemplateService', () => {
  let httpTestingController: HttpTestingController;
  let notificationTemplateService: NotificationTemplateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    notificationTemplateService = TestBed.inject<NotificationTemplateService>(NotificationTemplateService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('search', () => {
    it('should return the list of NotificationTemplate', done => {
      const hook = 'hook1';
      const scope = 'scope1';
      const notificationTemplates = [fakeNotificationTemplate()];

      notificationTemplateService.search({ scope, hook }).subscribe(response => {
        expect(response).toEqual(notificationTemplates);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates?scope=scope1&hook=hook1`,
      );
      expect(req.request.method).toEqual('GET');
      req.flush(notificationTemplates);
    });

    it('should works without search params', done => {
      const notificationTemplates = [fakeNotificationTemplate()];

      notificationTemplateService.search().subscribe(response => {
        expect(response).toEqual(notificationTemplates);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates`);
      expect(req.request.method).toEqual('GET');
      req.flush(notificationTemplates);
    });

    it('should works with empty search params', done => {
      const notificationTemplates = [fakeNotificationTemplate()];

      notificationTemplateService
        .search({
          scope: '',
          hook: '',
        })
        .subscribe(response => {
          expect(response).toEqual(notificationTemplates);
          done();
        });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates?scope=&hook=`);
      expect(req.request.method).toEqual('GET');
      req.flush(notificationTemplates);
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      const notificationTemplate = {
        content: 'Content',
        created_at: 1633417938291,
        description: 'Email sent to support team of an API or of the platform, when a support ticket is created.',
        hook: 'SUPPORT_TICKET',
        name: 'Support ticket',
        scope: 'TEMPLATES_FOR_ACTION',
        title: '${ticketSubject}',
        type: 'EMAIL',
      } as const;

      const createdNotificationTemplate = fakeNotificationTemplate({ ...notificationTemplate, id: 'templateId' });

      notificationTemplateService.create(notificationTemplate).subscribe(response => {
        expect(response).toEqual(createdNotificationTemplate);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(notificationTemplate);
      req.flush(createdNotificationTemplate);
    });
  });

  describe('update', () => {
    it('should call the API', done => {
      const updatedNotificationTemplate = fakeNotificationTemplate();

      notificationTemplateService.update(updatedNotificationTemplate).subscribe(response => {
        expect(response).toEqual(updatedNotificationTemplate);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates/${updatedNotificationTemplate.id}`,
      );
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual(updatedNotificationTemplate);
      req.flush(updatedNotificationTemplate);
    });
  });
});
