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

import { PortalConfigurationService } from './portal-configuration.service';

import { fakePortalConfiguration } from '../entities/portal/portalSettings.fixture';
import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('PortalConfigurationService', () => {
  let service: PortalConfigurationService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<PortalConfigurationService>(PortalConfigurationService);
  });

  describe('get', () => {
    it('should call the API', done => {
      const portalConfigurationToGet = fakePortalConfiguration();

      service.get().subscribe(portalSettings => {
        expect(portalSettings).toMatchObject(portalConfigurationToGet);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/portal`,
        })
        .flush(portalConfigurationToGet);
    });
  });

  describe('getByEnvConfiguration', () => {
    it('should call the API', done => {
      const portalConfigurationToGet = fakePortalConfiguration();

      service.getByEnvironmentId('custom_env').subscribe(portalSettings => {
        expect(portalSettings).toMatchObject(portalConfigurationToGet);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/environments/custom_env/portal`,
        })
        .flush(portalConfigurationToGet);
    });
  });
});
