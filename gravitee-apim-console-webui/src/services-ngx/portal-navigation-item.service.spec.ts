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

import { PortalNavigationItemService } from './portal-navigation-item.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  fakePortalNavigationItemsResponse,
  fakePortalNavigationPage,
  fakePortalNavigationFolder,
  fakePortalNavigationLink,
  fakeNewPagePortalNavigationItem,
  fakeNewFolderPortalNavigationItem,
  fakeNewLinkPortalNavigationItem,
} from '../entities/management-api-v2';

describe('PortalNavigationItemService', () => {
  let httpTestingController: HttpTestingController;
  let service: PortalNavigationItemService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PortalNavigationItemService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getNavigationItems', () => {
    it('should call the API', done => {
      const fakeResponse = fakePortalNavigationItemsResponse();
      service.getNavigationItems('TOP_NAVBAR').subscribe(response => {
        expect(response).toMatchObject(fakeResponse);
        done();
      });

      httpTestingController
        .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=TOP_NAVBAR` })
        .flush(fakeResponse);
    });
  });

  describe('createNavigationItem', () => {
    it('should create a PAGE navigation item', done => {
      const newPageItem = fakeNewPagePortalNavigationItem();
      const fakeCreatedItem = fakePortalNavigationPage({ title: newPageItem.title });

      service.createNavigationItem(newPageItem).subscribe(response => {
        expect(response).toMatchObject(fakeCreatedItem);
        expect(response.type).toBe('PAGE');
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items`,
      });
      expect(req.request.body).toEqual(newPageItem);
      req.flush(fakeCreatedItem);
    });

    it('should create a FOLDER navigation item', done => {
      const newFolderItem = fakeNewFolderPortalNavigationItem();
      const fakeCreatedItem = fakePortalNavigationFolder({ title: newFolderItem.title });

      service.createNavigationItem(newFolderItem).subscribe(response => {
        expect(response).toMatchObject(fakeCreatedItem);
        expect(response.type).toBe('FOLDER');
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items`,
      });
      expect(req.request.body).toEqual(newFolderItem);
      req.flush(fakeCreatedItem);
    });

    it('should create a LINK navigation item', done => {
      const newLinkItem = fakeNewLinkPortalNavigationItem();
      const fakeCreatedItem = fakePortalNavigationLink({ title: newLinkItem.title });

      service.createNavigationItem(newLinkItem).subscribe(response => {
        expect(response).toMatchObject(fakeCreatedItem);
        expect(response.type).toBe('LINK');
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items`,
      });
      expect(req.request.body).toEqual(newLinkItem);
      req.flush(fakeCreatedItem);
    });
  });
});
