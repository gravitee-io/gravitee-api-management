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
import { take } from 'rxjs/operators';

import { ApplicationService } from './application.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeApplication, fakeApplicationType } from '../entities/application/Application.fixture';
import { fakeSubscriptionPage } from '../entities/subscription/subscription.fixture';
import { fakeApplicationSubscriptionApiKey } from '../entities/subscription/ApplicationSubscriptionApiKey.fixture';

describe('ApplicationService', () => {
  let httpTestingController: HttpTestingController;
  let applicationService: ApplicationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    applicationService = TestBed.inject<ApplicationService>(ApplicationService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getAll', () => {
    it('should call the API', done => {
      const mockApplications = [fakeApplication()];

      applicationService.getAll().subscribe(response => {
        expect(response).toMatchObject(mockApplications);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications?status=active&exclude=picture&exclude=owner`,
      });

      req.flush(mockApplications);
    });

    it('should call the API with environmentId', done => {
      const mockApplications = [fakeApplication()];
      const environmentId = 'environmentId';

      applicationService.getAll({ environmentId }).subscribe(response => {
        expect(response).toMatchObject(mockApplications);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/environments/${environmentId}/applications?status=active&exclude=picture&exclude=owner`,
      });

      req.flush(mockApplications);
    });
  });

  describe('search', () => {
    it('should search application', done => {
      const mockApplications = [fakeApplication()];

      applicationService.list().subscribe(response => {
        expect(response).toMatchObject(mockApplications);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=10`,
      });

      req.flush(mockApplications);
    });

    it('should search application with application id', done => {
      const mockApplications = [fakeApplication()];
      const query = '0d93fd04-e834-447f-93fd-04e834047f9d';

      applicationService.list(undefined, query).subscribe(response => {
        expect(response).toMatchObject(mockApplications);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=10&query=0d93fd04-e834-447f-93fd-04e834047f9d`,
      });

      req.flush(mockApplications);
    });
  });

  describe('getById', () => {
    it('should call the API', done => {
      const mockApplication = fakeApplication({ id: 'my-app-id' });

      applicationService.getById('my-app-id').subscribe(response => {
        expect(response).toMatchObject(mockApplication);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/my-app-id`,
      });

      req.flush(mockApplication);
    });
  });

  describe('getApplicationType', () => {
    it('should call the API', done => {
      const mockApplication = fakeApplicationType({ id: 'my-app-id' });

      applicationService.getApplicationType('my-app-id').subscribe(response => {
        expect(response).toMatchObject(mockApplication);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/my-app-id/configuration`,
      });

      req.flush(mockApplication);
    });
  });
  describe('update', () => {
    it('should call the API', done => {
      const mockApplication = fakeApplication({ id: 'my-app-id' });

      applicationService.update(mockApplication).subscribe(response => {
        expect(response).toMatchObject(mockApplication);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/my-app-id`,
      });

      expect(req.request.body).toEqual({
        name: mockApplication.name,
        description: mockApplication.description,
        domain: mockApplication.domain,
        groups: mockApplication.groups,
        settings: mockApplication.settings,
        disable_membership_notifications: mockApplication.disable_membership_notifications,
        api_key_mode: mockApplication.api_key_mode,
      });

      req.flush(mockApplication);
    });
  });

  describe('findByIds', () => {
    it('should call the API', done => {
      const app1 = fakeApplication({ id: '1' });
      const app2 = fakeApplication({ id: '2' });
      const mockApplications = [app1, app2];

      applicationService.findByIds([app1.id, app2.id]).subscribe(response => {
        expect(response).toMatchObject(mockApplications);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=10&ids=1&ids=2`,
      });

      req.flush(mockApplications);
    });

    it('should call the API with custom pagination', done => {
      const app1 = fakeApplication({ id: '1' });
      const app2 = fakeApplication({ id: '2' });
      const mockApplications = [app1, app2];

      applicationService.findByIds([app1.id, app2.id], 2, 42).subscribe(response => {
        expect(response).toMatchObject(mockApplications);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=2&size=42&ids=1&ids=2`,
      });

      req.flush(mockApplications);
    });
  });

  describe('getLastApplicationFetch', () => {
    it('should call the API', () => {
      const mockApplication = fakeApplication({ id: 'my-app-id' });

      const done: string[] = [];
      // First call
      applicationService.getLastApplicationFetch('my-app-id').subscribe(response => {
        expect(response).toMatchObject(mockApplication);
        done.push('first');
      });

      // Only one call as the second one should be cached
      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/my-app-id`,
        })
        .flush(mockApplication);

      applicationService.getLastApplicationFetch('my-app-id').subscribe(response => {
        expect(response).toMatchObject(mockApplication);
        done.push('second');
      });

      expect(done).toEqual(['first', 'second']);
    });

    it('should not use cache if applicationId is different', () => {
      const mockApplicationOne = fakeApplication({ id: 'my-app-id' });
      const mockApplicationTwo = fakeApplication({ id: 'another-app-id' });

      const done: string[] = [];
      // First call
      applicationService
        .getLastApplicationFetch('my-app-id')
        .pipe(take(1))
        .subscribe(response => {
          expect(response).toMatchObject(mockApplicationOne);
          done.push('first');
        });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/my-app-id`,
        })
        .flush(mockApplicationOne);

      applicationService
        .getLastApplicationFetch('another-app-id')
        .pipe(take(1))
        .subscribe(response => {
          expect(response).toMatchObject(mockApplicationTwo);
          done.push('second');
        });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/another-app-id`,
        })
        .flush(mockApplicationTwo);

      expect(done).toEqual(['first', 'second']);
    });

    it('should refresh last application fetch', () => {
      const mockApplication = fakeApplication({ id: 'my-app-id' });

      const done: string[] = [];
      applicationService.getLastApplicationFetch('my-app-id').subscribe(response => {
        expect(response).toMatchObject(mockApplication);
        done.push('OK');
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/my-app-id`,
        })
        .flush(mockApplication);

      applicationService.refreshLastApplicationFetch();

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/my-app-id`,
        })
        .flush(mockApplication);

      expect(done).toEqual(['OK', 'OK']);
    });
  });

  describe('getSubscriptionsPage', () => {
    const appId = 'my-app-id';
    const subscriptions = [fakeSubscriptionPage({ application: appId })];

    it('should get application paged result', done => {
      applicationService.getSubscriptionsPage(appId).subscribe(response => {
        expect(response).toMatchObject(subscriptions);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${appId}/subscriptions?page=1&size=20`,
        })
        .flush(subscriptions);
    });

    it('should get application page 2 result', done => {
      applicationService.getSubscriptionsPage(appId, null, 2, 50).subscribe(response => {
        expect(response).toMatchObject(subscriptions);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${appId}/subscriptions?page=2&size=50`,
        })
        .flush(subscriptions);
    });

    it('should get application paged result for status', done => {
      applicationService.getSubscriptionsPage(appId, { status: ['ACCEPTED'] }).subscribe(response => {
        expect(response).toMatchObject(subscriptions);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${appId}/subscriptions?page=1&size=20&status=ACCEPTED`,
        })
        .flush(subscriptions);
    });

    it('should get application paged result for apiKey', done => {
      applicationService.getSubscriptionsPage(appId, { apiKey: 'azerty' }).subscribe(response => {
        expect(response).toMatchObject(subscriptions);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${appId}/subscriptions?page=1&size=20&api_key=azerty`,
        })
        .flush(subscriptions);
    });

    it('should get application paged result for apis', done => {
      applicationService.getSubscriptionsPage(appId, { apis: ['api-1', 'api-2'] }).subscribe(response => {
        expect(response).toMatchObject(subscriptions);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${appId}/subscriptions?page=1&size=20&api=api-1,api-2`,
        })
        .flush(subscriptions);
    });

    it('should get application paged result for API_KEY security type', done => {
      applicationService.getSubscriptionsPage(appId, { security_types: ['API_KEY'] }).subscribe(response => {
        expect(response).toMatchObject(subscriptions);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${appId}/subscriptions?page=1&size=20&security_types=API_KEY`,
        })
        .flush(subscriptions);
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      const mockApplication = {
        name: 'my-app',
        description: 'my-app-description',
        domain: 'my-app-domain',
        settings: {
          oauth: {
            application_type: 'WEB',
            grant_types: ['authorization_code', 'refresh_token'],
            redirect_uris: ['redirectUri'],
          },
        },
      };

      applicationService.create(mockApplication).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications`,
      });

      expect(req.request.body).toEqual(mockApplication);
      req.flush({});
    });
  });

  describe('getApiKeys', () => {
    it('should call the API', done => {
      applicationService.getApiKeys('applicationId').subscribe(apiKeys => {
        expect(apiKeys).toEqual([fakeApplicationSubscriptionApiKey()]);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/applicationId/apikeys`,
        })
        .flush([fakeApplicationSubscriptionApiKey()]);
    });
  });

  describe('renewApiKey', () => {
    it('should call the API', done => {
      applicationService.renewApiKey('applicationId').subscribe(() => {
        done();
      });

      httpTestingController
        .expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/applicationId/apikeys/_renew`,
        })
        .flush({});
    });
  });

  describe('revokeApiKey', () => {
    it('should call the API', done => {
      applicationService.revokeApiKey('applicationId', 'apiKeyId').subscribe(() => {
        done();
      });

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/applicationId/apikeys/apiKeyId`,
        })
        .flush({});
    });
  });
});
