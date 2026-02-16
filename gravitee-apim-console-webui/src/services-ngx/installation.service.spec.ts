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

import { InstallationService } from './installation.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeInstallation } from '../entities/installation/installation.fixture';

describe('InstallationService', () => {
  let httpTestingController: HttpTestingController;
  let installationService: InstallationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    installationService = TestBed.inject<InstallationService>(InstallationService);
  });

  describe('get', () => {
    it('should call the API', done => {
      const installation = fakeInstallation();

      installationService.get().subscribe(response => {
        expect(response).toMatchObject(installation);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`);
      expect(req.request.method).toEqual('GET');

      req.flush(installation);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
