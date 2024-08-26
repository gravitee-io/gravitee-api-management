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

import { UiPortalMenuLinksService } from './ui-portal-menu-links.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  CreatePortalMenuLink,
  fakeCreatePortalMenuLink,
  fakePagedResult,
  fakePortalMenuLink,
  fakeUpdatePortalMenuLink,
  PagedResult,
  PortalMenuLink,
  UpdatePortalMenuLink,
} from '../entities/management-api-v2';

describe('UiPortalMenuLinkService', () => {
  let httpTestingController: HttpTestingController;
  let service: UiPortalMenuLinksService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<UiPortalMenuLinksService>(UiPortalMenuLinksService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API', (done) => {
      service.list().subscribe((portalMenuLinks) => {
        expect(portalMenuLinks.data.length).toEqual(1);
        done();
      });

      expectListPortalMenuLinksRequest(httpTestingController, fakePagedResult([fakePortalMenuLink()]));
    });
  });

  describe('create', () => {
    it('should call the API', (done) => {
      service.create(fakeCreatePortalMenuLink()).subscribe((pml) => {
        expect(pml).toBeTruthy();
        done();
      });

      expectCreatePortalMenuLinkRequest(httpTestingController, fakeCreatePortalMenuLink());
    });
  });

  describe('get', () => {
    it('should call the API', (done) => {
      service.get(fakePortalMenuLink().id).subscribe((pml) => {
        expect(pml).toBeTruthy();
        done();
      });

      expectGetPortalMenuLinkRequest(httpTestingController, fakePortalMenuLink());
    });
  });

  describe('update', () => {
    it('should call the API', (done) => {
      service.update('pmlId', fakeUpdatePortalMenuLink()).subscribe((pml) => {
        expect(pml).toBeTruthy();
        done();
      });

      expectUpdatePortalMenuLinkRequest(httpTestingController, 'pmlId', fakeUpdatePortalMenuLink());
    });
  });

  describe('delete', () => {
    it('should call the API', (done) => {
      service.delete('pmlId').subscribe(() => {
        done();
      });
      expectDeletePortalMenuLinkRequest(httpTestingController, 'pmlId');
    });
  });
});

export const expectListPortalMenuLinksRequest = (
  httpTestingController: HttpTestingController,
  portalMenuLinks: PagedResult<PortalMenuLink> = fakePagedResult([fakePortalMenuLink()]),
  queryParams: string = '?page=1&perPage=10',
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links${queryParams}`);
  expect(req.request.method).toEqual('GET');
  req.flush(portalMenuLinks);
};

export const expectGetPortalMenuLinkRequest = (
  httpTestingController: HttpTestingController,
  portalMenuLink: PortalMenuLink = fakePortalMenuLink(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links/${portalMenuLink.id}`);
  expect(req.request.method).toEqual('GET');
  req.flush(portalMenuLink);
};

export const expectCreatePortalMenuLinkRequest = (
  httpTestingController: HttpTestingController,
  expectedCreatePortalMenuLink: CreatePortalMenuLink,
  portalMenuLinkCreated: PortalMenuLink = fakePortalMenuLink(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links`);
  expect(req.request.method).toEqual('POST');
  expect(req.request.body).toStrictEqual(expectedCreatePortalMenuLink);
  req.flush(portalMenuLinkCreated);
};

export const expectUpdatePortalMenuLinkRequest = (
  httpTestingController: HttpTestingController,
  portalMenuLinkId: string,
  expectedUpdatePortalMenuLink: UpdatePortalMenuLink,
  portalMenuLinkUpdated: PortalMenuLink = fakePortalMenuLink(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links/${portalMenuLinkId}`);
  expect(req.request.method).toEqual('PUT');
  expect(req.request.body).toStrictEqual(expectedUpdatePortalMenuLink);
  req.flush(portalMenuLinkUpdated);
};

export const expectDeletePortalMenuLinkRequest = (httpTestingController: HttpTestingController, portalMenuLinkId: string) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links/${portalMenuLinkId}`);
  expect(req.request.method).toEqual('DELETE');
  req.flush({});
};
