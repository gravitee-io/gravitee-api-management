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
import { HttpClientTestingModule, HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ConfigService } from './config.service';
import { ConfigurationPortal } from '../entities/configuration/configuration-portal';
import { ConfigurationPortalNext } from '../entities/configuration/configuration-portal-next';

describe('ConfigService', () => {
  let service: ConfigService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [provideHttpClientTesting()],
    });
    service = TestBed.inject(ConfigService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe('initBaseURL', () => {
    it('should call base url without enforced env id', done => {
      const configJson = {
        baseURL: 'http://localhost:8083/portal',
      };

      const configBootstrap = {
        ...configJson,
        environmentId: 'DEFAULT',
      };

      service.initBaseURL().subscribe(baseUrl => {
        expect(baseUrl).toEqual('http://localhost:8083/portal/environments/DEFAULT');
        done();
      });

      httpTestingController.expectOne('./assets/config.json').flush(configJson);
      httpTestingController.expectOne('http://localhost:8083/portal/ui/bootstrap').flush(configBootstrap);
    });

    it('should call base url with enforced env id in baseURL', done => {
      const configJson = {
        baseURL: 'http://localhost:8083/portal/environments/DEFAULT',
      };

      const configBootstrap = {
        baseURL: 'http://localhost:8083/portal',
        environmentId: 'DEFAULT',
      };

      service.initBaseURL().subscribe(baseUrl => {
        expect(baseUrl).toEqual('http://localhost:8083/portal/environments/DEFAULT');
        done();
      });

      httpTestingController.expectOne('./assets/config.json').flush(configJson);
      httpTestingController.expectOne('http://localhost:8083/portal/ui/bootstrap?environmentId=DEFAULT').flush(configBootstrap);
    });

    it('should call base url with enforced env id in environmentId', done => {
      const configJson = {
        baseURL: 'http://localhost:8083/portal',
        environmentId: 'DEFAULT',
      };

      const configBootstrap = {
        baseURL: 'http://localhost:8083/portal',
        environmentId: 'DEFAULT',
      };

      service.initBaseURL().subscribe(baseUrl => {
        expect(baseUrl).toEqual('http://localhost:8083/portal/environments/DEFAULT');
        done();
      });

      httpTestingController.expectOne('./assets/config.json').flush(configJson);
      httpTestingController.expectOne('http://localhost:8083/portal/ui/bootstrap?environmentId=DEFAULT').flush(configBootstrap);
    });
  });

  describe('loadConfiguration', () => {
    describe('load portalNext configuration', () => {
      it('should load portal next configuration', done => {
        const portalNext: ConfigurationPortalNext = {
          siteTitle: 'a site title',
          banner: {
            enabled: true,
            title: 'a title',
            subtitle: 'a subtitle',
          },
        };
        service.loadConfiguration().subscribe(() => {
          expect(service.configuration.portalNext).toEqual(portalNext);
          done();
        });

        httpTestingController.expectOne(`/configuration`).flush({ portalNext });
      });

      it('should load missing portal next configuration', done => {
        const portalNext: ConfigurationPortalNext = {};
        service.loadConfiguration().subscribe(() => {
          expect(service.configuration.portalNext).toEqual(portalNext);
          done();
        });

        httpTestingController.expectOne(`/configuration`).flush({ portalNext: {} });
      });
    });

    describe('load portal configuration', () => {
      it('should load portal configuration', done => {
        const portal: ConfigurationPortal = {
          apikeyHeader: 'X-My-Apikey',
        };
        service.loadConfiguration().subscribe(() => {
          expect(service.configuration.portal).toEqual(portal);
          done();
        });

        httpTestingController.expectOne(`/configuration`).flush({ portal });
      });

      it('should load missing portal configuration', done => {
        const portal: ConfigurationPortal = {};
        service.loadConfiguration().subscribe(() => {
          expect(service.configuration.portal).toEqual(portal);
          done();
        });

        httpTestingController.expectOne(`/configuration`).flush({ portal: {} });
      });
    });
  });
});
