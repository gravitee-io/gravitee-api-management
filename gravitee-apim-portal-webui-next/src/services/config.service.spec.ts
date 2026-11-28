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
import { firstValueFrom } from 'rxjs';

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

  afterEach(() => httpTestingController.verify());

  describe('initBaseURL', () => {
    it('should call base url without enforced env id', done => {
      const configJson = {
        baseURL: 'http://localhost:8083/portal',
      };

      const configBootstrap = {
        ...configJson,
        environmentId: 'DEFAULT',
        portalId: 'default-portal',
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
        portalId: 'default-portal',
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
        portalId: 'default-portal',
      };

      service.initBaseURL().subscribe(baseUrl => {
        expect(baseUrl).toEqual('http://localhost:8083/portal/environments/DEFAULT');
        done();
      });

      httpTestingController.expectOne('./assets/config.json').flush(configJson);
      httpTestingController.expectOne('http://localhost:8083/portal/ui/bootstrap?environmentId=DEFAULT').flush(configBootstrap);
    });
  });

  describe('portal context', () => {
    const bootstrapUrl = 'https://api.example.com/portal/ui/bootstrap?environmentId=OTHER';
    const environmentBaseURL = 'https://resolved.example.com/portal/environments/OTHER';

    it('rejects portalBaseURL access before bootstrap resolves the portal', () => {
      expect(() => service.portalBaseURL).toThrow('Portal ID is required');
    });

    it.each([
      {
        bootstrapPortalId: 'bootstrap-portal',
        expectedPortalId: 'bootstrap-portal',
        expectedPortalBaseURL: 'https://resolved.example.com/portal/environments/OTHER/portal/bootstrap-portal',
      },
      {
        bootstrapPortalId: '  bootstrap-portal  ',
        expectedPortalId: 'bootstrap-portal',
        expectedPortalBaseURL: 'https://resolved.example.com/portal/environments/OTHER/portal/bootstrap-portal',
      },
      {
        bootstrapPortalId: 'folder/portal?query=value',
        expectedPortalId: 'folder/portal?query=value',
        expectedPortalBaseURL: 'https://resolved.example.com/portal/environments/OTHER/portal/folder%2Fportal%3Fquery%3Dvalue',
      },
    ])(
      'resolves bootstrap portal $bootstrapPortalId in the bootstrap environment',
      async ({ bootstrapPortalId, expectedPortalId, expectedPortalBaseURL }) => {
        const initialized = firstValueFrom(service.initBaseURL());
        httpTestingController.expectOne('./assets/config.json').flush({
          baseURL: 'https://api.example.com/portal/environments/IGNORED/',
          environmentId: 'OTHER',
        });
        httpTestingController.expectOne(bootstrapUrl).flush({
          baseURL: 'https://resolved.example.com/portal/',
          environmentId: 'OTHER',
          portalId: bootstrapPortalId,
        });

        expect(await initialized).toEqual(environmentBaseURL);
        expect(service.baseURL).toEqual(environmentBaseURL);
        expect(service.portalId).toEqual(expectedPortalId);
        expect(service.portalBaseURL).toEqual(expectedPortalBaseURL);
      },
    );

    it('selects the configured portal over the bootstrap portal', async () => {
      const initialized = firstValueFrom(service.initBaseURL());
      httpTestingController.expectOne('./assets/config.json').flush({
        baseURL: 'https://api.example.com/portal/environments/IGNORED/',
        environmentId: 'OTHER',
        portalId: 'portal-b',
      });
      httpTestingController.expectOne(bootstrapUrl).flush({
        baseURL: 'https://resolved.example.com/portal/',
        environmentId: 'OTHER',
        portalId: 'portal-a',
      });

      expect(await initialized).toEqual(environmentBaseURL);
      expect(service.baseURL).toEqual(environmentBaseURL);
      expect(service.portalId).toEqual('portal-b');
      expect(service.portalBaseURL).toEqual(`${environmentBaseURL}/portal/portal-b`);
    });

    it('trims the configured portal ID', async () => {
      const initialized = firstValueFrom(service.initBaseURL());
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal', portalId: '  portal-b  ' });
      httpTestingController.expectOne('/portal/ui/bootstrap').flush({ baseURL: '/portal', environmentId: 'DEFAULT', portalId: 'portal-a' });

      await initialized;
      expect(service.portalId).toEqual('portal-b');
      expect(service.portalBaseURL).toEqual('/portal/environments/DEFAULT/portal/portal-b');
    });

    it.each([undefined, null, '', '   '])('uses the bootstrap portal when the configured portal ID is %p', async portalId => {
      const initialized = firstValueFrom(service.initBaseURL());
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal', portalId });
      httpTestingController.expectOne('/portal/ui/bootstrap').flush({ baseURL: '/portal', environmentId: 'DEFAULT', portalId: 'portal-a' });

      await initialized;
      expect(service.portalId).toEqual('portal-a');
      expect(service.portalBaseURL).toEqual('/portal/environments/DEFAULT/portal/portal-a');
    });

    it.each([undefined, null, '', '   '])('selects the configured portal when the bootstrap has none (%p)', async bootstrapPortalId => {
      const initialized = firstValueFrom(service.initBaseURL());
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal', portalId: 'portal-b' });
      httpTestingController
        .expectOne('/portal/ui/bootstrap')
        .flush({ baseURL: '/portal', environmentId: 'DEFAULT', portalId: bootstrapPortalId });

      expect(await initialized).toEqual('/portal/environments/DEFAULT');
      expect(service.portalId).toEqual('portal-b');
      expect(service.portalBaseURL).toEqual('/portal/environments/DEFAULT/portal/portal-b');
    });

    it.each([12, true, {}, []])('fails startup on a configured portal ID of another type %p', async portalId => {
      const initialized = firstValueFrom(service.initBaseURL());
      const rejected = expect(initialized).rejects.toThrow('Portal ID must be a string');
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal', portalId });
      httpTestingController.expectNone('/portal/ui/bootstrap');
      await rejected;
      expect(service.portalId).toEqual('');
    });

    it.each(['.', '..', ' . ', ' .. '])('fails startup on a configured dot-segment portal ID %p', async portalId => {
      const initialized = firstValueFrom(service.initBaseURL());
      const rejected = expect(initialized).rejects.toThrow('Portal ID must not be a dot segment');
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal', portalId });
      httpTestingController.expectNone('/portal/ui/bootstrap');
      await rejected;
      expect(service.portalId).toEqual('');
    });

    it('does not bypass a failed bootstrap with a configured portal ID', async () => {
      const initialized = firstValueFrom(service.initBaseURL());
      const rejected = expect(initialized).rejects.toMatchObject({ status: 503 });
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal', portalId: 'portal-b' });
      httpTestingController.expectOne('/portal/ui/bootstrap').flush({}, { status: 503, statusText: 'Service Unavailable' });
      await rejected;
      expect(service.portalId).toEqual('');
      expect(service.baseURL).toEqual('');
    });

    it.each([
      { bootstrapPortalId: 12, expectedError: 'Portal ID must be a string' },
      { bootstrapPortalId: '..', expectedError: 'Portal ID must not be a dot segment' },
    ])(
      'does not bypass an invalid bootstrap portal ID $bootstrapPortalId with a configured portal ID',
      async ({ bootstrapPortalId, expectedError }) => {
        const initialized = firstValueFrom(service.initBaseURL());
        const rejected = expect(initialized).rejects.toThrow(expectedError);
        httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal', portalId: 'portal-b' });
        httpTestingController
          .expectOne('/portal/ui/bootstrap')
          .flush({ baseURL: '/portal', environmentId: 'DEFAULT', portalId: bootstrapPortalId });
        await rejected;
        expect(service.portalId).toEqual('');
      },
    );

    it('encodes the configured portal ID as one path segment', async () => {
      const initialized = firstValueFrom(service.initBaseURL());
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal', portalId: 'portal b/../x' });
      httpTestingController.expectOne('/portal/ui/bootstrap').flush({ baseURL: '/portal', environmentId: 'DEFAULT', portalId: 'portal-a' });

      await initialized;
      expect(service.portalBaseURL).toEqual('/portal/environments/DEFAULT/portal/portal%20b%2F..%2Fx');
    });

    it.each([undefined, null, '', '   '])('rejects missing portal context %p', async portalId => {
      const initialized = firstValueFrom(service.initBaseURL());
      const rejected = expect(initialized).rejects.toThrow('Portal ID is required');
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal' });
      httpTestingController.expectOne('/portal/ui/bootstrap').flush({ baseURL: '/portal', environmentId: 'DEFAULT', portalId });
      await rejected;
      expect(service.baseURL).toEqual('');
    });

    it.each([12, true, {}, []])('rejects invalid bootstrap portal ID %p', async portalId => {
      const initialized = firstValueFrom(service.initBaseURL());
      const rejected = expect(initialized).rejects.toThrow('Portal ID must be a string');
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal' });
      httpTestingController.expectOne('/portal/ui/bootstrap').flush({ baseURL: '/portal', environmentId: 'DEFAULT', portalId });
      await rejected;
    });

    it.each(['.', '..', ' . ', ' .. '])('rejects dot-segment portal ID %p so requests cannot escape the portal scope', async portalId => {
      const initialized = firstValueFrom(service.initBaseURL());
      const rejected = expect(initialized).rejects.toThrow('Portal ID must not be a dot segment');
      httpTestingController.expectOne('./assets/config.json').flush({ baseURL: '/portal' });
      httpTestingController.expectOne('/portal/ui/bootstrap').flush({ baseURL: '/portal', environmentId: 'DEFAULT', portalId });
      await rejected;
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

      it('should load the skip login with single IdP setting', done => {
        const portalNext: ConfigurationPortalNext = {
          skipLoginWithSingleIdp: { enabled: true },
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
