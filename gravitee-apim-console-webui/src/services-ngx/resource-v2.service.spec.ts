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

import { ResourceV2Service } from './resource-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeResourcePlugins } from '../entities/management-api-v2';

describe('ResourceV2Service', () => {
  let httpTestingController: HttpTestingController;
  let resourceService: ResourceV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    resourceService = TestBed.inject<ResourceV2Service>(ResourceV2Service);
  });

  describe('list', () => {
    it('should call the API', (done) => {
      const resources = fakeResourcePlugins();

      resourceService.list().subscribe((response) => {
        expect(response).toStrictEqual(resources);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/resources`);
      expect(req.request.method).toEqual('GET');

      req.flush(resources);
    });
  });

  describe('getSchema', () => {
    it('should call the API', (done) => {
      const resourceId = 'resource#1';
      const resourceSchema = {};

      resourceService.getSchema(resourceId).subscribe((response) => {
        expect(response).toStrictEqual(resourceSchema);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/resources/resource#1/schema`);
      expect(req.request.method).toEqual('GET');

      req.flush(resourceSchema);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
