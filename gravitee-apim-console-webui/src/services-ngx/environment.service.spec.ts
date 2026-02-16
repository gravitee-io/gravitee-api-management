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

import { EnvironmentService } from './environment.service';

import { fakeEnvironment } from '../entities/environment/environment.fixture';
import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('EnvironmentService', () => {
  let httpTestingController: HttpTestingController;
  let environmentService: EnvironmentService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    environmentService = TestBed.inject<EnvironmentService>(EnvironmentService);
  });

  describe('list', () => {
    it('should call the API', done => {
      const fakeEnvironments = [fakeEnvironment()];

      environmentService.list().subscribe(environments => {
        expect(environments).toMatchObject(fakeEnvironments);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeEnvironments);
    });
  });

  describe('getCurrent', () => {
    it('should call the API', done => {
      const environment = fakeEnvironment();

      environmentService.getCurrent().subscribe(env => {
        expect(env).toMatchObject(environment);
        done();
      });

      const req = httpTestingController.expectOne({
        url: CONSTANTS_TESTING.env.baseURL,
        method: 'GET',
      });

      req.flush(environment);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
