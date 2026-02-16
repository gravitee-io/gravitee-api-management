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

import { ResourceService } from './resource.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeResourceListItem } from '../entities/resource/resourceListItem.fixture';
import { fakeResourceDocumentation } from '../entities/resource/resourceDocumentation.fixture';

describe('ResourceService', () => {
  let httpTestingController: HttpTestingController;
  let resourceService: ResourceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    resourceService = TestBed.inject<ResourceService>(ResourceService);
  });

  describe('list', () => {
    it('should call the API with params to true', done => {
      const resources = [fakeResourceListItem()];

      resourceService
        .list({
          expandSchema: true,
          expandIcon: true,
        })
        .subscribe(response => {
          expect(response).toStrictEqual(resources);
          done();
        });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/resources?expand=schema&expand=icon`,
        })
        .flush(resources);
    });

    it('should call the API with different params', done => {
      const resources = [fakeResourceListItem()];

      resourceService
        .list({
          expandIcon: false,
        })
        .subscribe(response => {
          expect(response).toStrictEqual(resources);
          done();
        });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/resources`,
        })
        .flush(resources);
    });
  });

  describe('getDocumentation', () => {
    it('should call the API', done => {
      const resourceId = 'resource#1';
      const resourceDocumentation = fakeResourceDocumentation();

      resourceService.getDocumentation(resourceId).subscribe(response => {
        expect(response).toStrictEqual(resourceDocumentation);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/resources/resource#1/documentation`,
        })
        .flush(resourceDocumentation);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
