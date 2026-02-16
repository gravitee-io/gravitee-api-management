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

import { PortalPagesService } from './portal-pages.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakePortalPageWithDetails } from '../entities/portal/portal-page-with-details.fixture';

describe('PortalPagesService', () => {
  let httpTestingController: HttpTestingController;
  let portalPagesService: PortalPagesService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    portalPagesService = TestBed.inject<PortalPagesService>(PortalPagesService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('updatePortalPage', () => {
    it('should call the API', done => {
      const fakePortalPage = fakePortalPageWithDetails({ content: 'test' });
      const toUpdate = { content: 'test' };

      portalPagesService.patchPortalPage(fakePortalPage.id, toUpdate).subscribe(response => {
        expect(response).toStrictEqual(fakePortalPage);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PATCH',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/${fakePortalPage.id}`,
      });

      expect(req.request.body).toEqual(toUpdate);
      req.flush(fakePortalPage);
    });
  });

  describe('publishPage', () => {
    it('should call the API to publish a page', done => {
      const pageId = 'test-98';
      const fakePortalPage = fakePortalPageWithDetails({ published: true, id: pageId });

      portalPagesService.publishPage(fakePortalPage.id).subscribe(response => {
        expect(response).toStrictEqual(fakePortalPage);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/${fakePortalPage.id}/_publish`,
      });

      // Expect the body to be an empty object as sent by the service
      expect(req.request.body).toEqual({});
      req.flush(fakePortalPage);
    });
  });

  describe('unpublishPage', () => {
    it('should call the API to unpublish a page', done => {
      const pageId = 'test-99';
      const fakePortalPage = fakePortalPageWithDetails({ published: false, id: pageId });

      portalPagesService.unpublishPage(pageId).subscribe(response => {
        expect(response).toStrictEqual(fakePortalPage);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/${pageId}/_unpublish`,
      });

      // Expect the body to be an empty object
      expect(req.request.body).toEqual({});

      req.flush(fakePortalPage);
    });
  });
});
