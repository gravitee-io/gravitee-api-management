/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { PortalMenuLink, PortalMenuLinksService } from './portal-menu-links.service';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('PortalMenuLinksService', () => {
  let service: PortalMenuLinksService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(PortalMenuLinksService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should return links list', done => {
    const linkLists: PortalMenuLink[] = [
      {
        id: 'link-id-1',
        type: 'external',
        name: 'link-name-1',
        target: 'link-target-1',
        order: 1,
      },
      {
        id: 'link-id-2',
        type: 'external',
        name: 'link-name-2',
        target: 'link-target-2',
        order: 2,
      },
    ];
    service.loadCustomLinks().subscribe(response => {
      expect(response).toMatchObject(linkLists);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-menu-links`);
    expect(req.request.method).toEqual('GET');

    req.flush(linkLists);

    expect(service.links).toEqual(linkLists);
  });
});
