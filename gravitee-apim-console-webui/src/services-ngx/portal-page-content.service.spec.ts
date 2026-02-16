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
import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { PortalPageContentService } from './portal-page-content.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakePortalPageContent, fakeNewPortalPageContent } from '../entities/management-api-v2';

describe('PortalPageContentService', () => {
  let httpTestingController: HttpTestingController;
  let service: PortalPageContentService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PortalPageContentService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getPageContent', () => {
    it('should call the API', done => {
      const contentId = 'content-1';
      const fakePageContent = fakePortalPageContent({ id: contentId });

      service.getPageContent(contentId).subscribe(response => {
        expect(response).toMatchObject(fakePageContent);
        done();
      });

      httpTestingController
        .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/${contentId}` })
        .flush(fakePageContent);
    });
  });

  describe('createPageContent', () => {
    it('should call the API', done => {
      const newPageContent = fakeNewPortalPageContent();
      const fakeCreatedContent = fakePortalPageContent({ content: newPageContent.content });

      service.createPageContent(newPageContent).subscribe(response => {
        expect(response).toMatchObject(fakeCreatedContent);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents`,
      });
      expect(req.request.body).toEqual(newPageContent);
      req.flush(fakeCreatedContent);
    });
  });

  describe('updatePageContent', () => {
    it('should call the API', done => {
      const contentId = 'content-1';
      const updateContent = { content: 'Updated content' };
      const fakeUpdatedContent = fakePortalPageContent({ id: contentId, content: updateContent.content });

      service.updatePageContent(contentId, updateContent).subscribe(response => {
        expect(response).toMatchObject(fakeUpdatedContent);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/${contentId}`,
      });
      expect(req.request.body).toEqual(updateContent);
      req.flush(fakeUpdatedContent);
    });
  });
});
