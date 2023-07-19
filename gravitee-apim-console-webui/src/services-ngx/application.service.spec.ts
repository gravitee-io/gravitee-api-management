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

import { ApplicationService } from './application.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeApplication } from '../entities/application/Application.fixture';

describe('ApplicationService', () => {
  let httpTestingController: HttpTestingController;
  let applicationService: ApplicationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    applicationService = TestBed.inject<ApplicationService>(ApplicationService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getAll', () => {
    it('should call the API', (done) => {
      const mockApplications = [fakeApplication()];

      applicationService.getAll().subscribe((response) => {
        expect(response).toMatchObject(mockApplications);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications?status=active&exclude=picture&exclude=owner`,
      });

      req.flush(mockApplications);
    });

    it('should call the API with environmentId', (done) => {
      const mockApplications = [fakeApplication()];
      const environmentId = 'environmentId';

      applicationService.getAll({ environmentId }).subscribe((response) => {
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
    it('should search application', (done) => {
      const mockApplications = [fakeApplication()];

      applicationService.list().subscribe((response) => {
        expect(response).toMatchObject(mockApplications);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=10`,
      });

      req.flush(mockApplications);
    });

    it('should search application with application id', (done) => {
      const mockApplications = [fakeApplication()];
      const query = '0d93fd04-e834-447f-93fd-04e834047f9d';

      applicationService.list(undefined, query).subscribe((response) => {
        expect(response).toMatchObject(mockApplications);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=10&ids=0d93fd04-e834-447f-93fd-04e834047f9d`,
      });

      req.flush(mockApplications);
    });
  });

  describe('getById', () => {
    it('should call the API', (done) => {
      const mockApplication = fakeApplication({ id: 'my-app-id' });

      applicationService.getById('my-app-id').subscribe((response) => {
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
  describe('update', () => {
    it('should call the API', (done) => {
      const mockApplication = fakeApplication({ id: 'my-app-id' });

      applicationService.update(mockApplication).subscribe((response) => {
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
        picture_url: mockApplication.picture_url,
        disable_membership_notifications: mockApplication.disable_membership_notifications,
        api_key_mode: mockApplication.api_key_mode,
      });

      req.flush(mockApplication);
    });
  });
});
