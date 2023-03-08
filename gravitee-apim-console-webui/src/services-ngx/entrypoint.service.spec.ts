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

import { EntrypointService } from './entrypoint.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeEntrypoint } from '../entities/entrypoint/entrypoint.fixture';
import { fakeConnectorListItem } from '../entities/connector/connector-list-item.fixture';

describe('EntrypointService', () => {
  let httpTestingController: HttpTestingController;
  let entrypointService: EntrypointService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    entrypointService = TestBed.inject<EntrypointService>(EntrypointService);
  });

  describe('list', () => {
    it('should call the API', (done) => {
      const fakeEntrypoints = [fakeEntrypoint()];

      entrypointService.list().subscribe((environments) => {
        expect(environments).toMatchObject(fakeEntrypoints);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints`,
          method: 'GET',
        })
        .flush(fakeEntrypoints);
    });
  });

  describe('create', () => {
    it('should call the API', (done) => {
      const entrypointToCreate = fakeEntrypoint();

      entrypointService.create(entrypointToCreate).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/`,
        method: 'POST',
      });
      expect(req.request.body).toEqual(entrypointToCreate);
      req.flush(null);
    });
  });

  describe('update', () => {
    it('should call the API', (done) => {
      const entrypointToUpdate = fakeEntrypoint();

      entrypointService.update(entrypointToUpdate).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual(entrypointToUpdate);
      req.flush(null);
    });
  });

  describe('delete', () => {
    it('should call the API', (done) => {
      const entrypointToUpdate = fakeEntrypoint();

      entrypointService.delete(entrypointToUpdate.id).subscribe(() => {
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/${entrypointToUpdate.id}`,
          method: 'DELETE',
        })
        .flush(null);
    });
  });

  describe('v4ListSyncEntrypointPlugins', () => {
    it('should call the API', (done) => {
      const fakeConnectors = [fakeConnectorListItem({ supportedApiType: 'proxy' }), fakeConnectorListItem({ supportedApiType: 'message' })];

      entrypointService.v4ListSyncEntrypointPlugins().subscribe((connectors) => {
        expect(connectors).toMatchObject([fakeConnectors[0]]);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/v4/entrypoints?expand=icon`,
          method: 'GET',
        })
        .flush(fakeConnectors);
    });
  });

  describe('v4ListAsyncEntrypointPlugins', () => {
    it('should call the API', (done) => {
      const fakeConnectors = [fakeConnectorListItem({ supportedApiType: 'proxy' }), fakeConnectorListItem({ supportedApiType: 'message' })];

      entrypointService.v4ListAsyncEntrypointPlugins().subscribe((connectors) => {
        expect(connectors).toMatchObject([fakeConnectors[1]]);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/v4/entrypoints?expand=icon`,
          method: 'GET',
        })
        .flush(fakeConnectors);
    });

    it('should call the API with expand', (done) => {
      const fakeConnectors = [fakeConnectorListItem({ supportedApiType: 'proxy' }), fakeConnectorListItem({ supportedApiType: 'message' })];

      entrypointService.v4ListAsyncEntrypointPlugins(['schema', 'icon']).subscribe((connectors) => {
        expect(connectors).toMatchObject([fakeConnectors[1]]);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/v4/entrypoints?expand=schema&expand=icon`,
          method: 'GET',
        })
        .flush(fakeConnectors);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
