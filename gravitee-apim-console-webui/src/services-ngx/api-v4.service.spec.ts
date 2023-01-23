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

import { ApiV4Service } from './api-v4.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeApiEntity, fakeNewApiEntity } from '../entities/api-v4';

describe('ApiV4Service', () => {
  let httpTestingController: HttpTestingController;
  let apiV4Service: ApiV4Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiV4Service = TestBed.inject<ApiV4Service>(ApiV4Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('create', () => {
    it('should call the API', (done) => {
      const newApiEntity = fakeNewApiEntity();

      apiV4Service.create(newApiEntity).subscribe((api) => {
        expect(api.name).toEqual(newApiEntity.name);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/v4/apis`,
        method: 'POST',
      });

      req.flush(fakeApiEntity());
    });
  });

  describe('get', () => {
    it('should call the API', (done) => {
      const apiEntity = fakeApiEntity();

      apiV4Service.get(apiEntity.id).subscribe((api) => {
        expect(api.name).toEqual(apiEntity.name);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/v4/apis/${apiEntity.id}`,
        method: 'GET',
      });

      req.flush(fakeApiEntity());
    });
  });
});
