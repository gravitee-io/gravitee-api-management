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

import { DocumentationService } from './documentation.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { Page } from '../entities/page';

describe('DocumentationService', () => {
  let httpTestingController: HttpTestingController;
  let documentationService: DocumentationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    documentationService = TestBed.inject<DocumentationService>(DocumentationService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('search', () => {
    it('should call the API', done => {
      const API_ID = 'my-api-id';
      const fakePages: Page[] = [
        {
          id: 'page-id',
        },
      ];

      documentationService
        .apiSearch(API_ID, {
          api: 'api',
          homepage: true,
        })
        .subscribe(environments => {
          expect(environments).toMatchObject(fakePages);
          done();
        });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/pages?api=api&homepage=true`,
          method: 'GET',
        })
        .flush(fakePages);
    });
  });
});
