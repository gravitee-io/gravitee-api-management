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

import { ApiDocumentationV2Service } from './api-documentation-v2.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { CreateDocumentation, CreateDocumentationFolder } from '../entities/management-api-v2/documentation/createDocumentation';
import { Page } from '../entities/management-api-v2/documentation/page';

describe('ApiDocumentationV2Service', () => {
  let httpTestingController: HttpTestingController;
  let service: ApiDocumentationV2Service;
  const API_ID = 'api-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiDocumentationV2Service>(ApiDocumentationV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getApiPages', () => {
    it('should call the API', (done) => {
      const fakeResponse = [];

      service.getApiPages(API_ID).subscribe((response) => {
        expect(response).toEqual(fakeResponse);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
        method: 'GET',
      });

      req.flush(fakeResponse);
    });
  });

  describe('createDocumentation', () => {
    it('should call the API to create folder', (done) => {
      const fakeResponse: Page = {};
      const createFolder: CreateDocumentationFolder = {
        type: 'FOLDER',
        name: 'folder',
        visibility: 'PUBLIC',
      };
      service.createDocumentationPage(API_ID, createFolder).subscribe((response) => {
        expect(response).toEqual(fakeResponse);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
        method: 'POST',
      });

      req.flush(fakeResponse);
    });
    it('should call the API to create page', (done) => {
      const fakeResponse: Page = {};
      const createPage: CreateDocumentation = {
        type: 'MARKDOWN',
        visibility: 'PRIVATE',
        name: 'page',
        content: '#TITLE \n content',
      };
      service.createDocumentationPage(API_ID, createPage).subscribe((response) => {
        expect(response).toEqual(fakeResponse);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
        method: 'POST',
      });

      req.flush(fakeResponse);
    });
  });
});
