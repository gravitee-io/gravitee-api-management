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
import { take } from 'rxjs/operators';

import { ApiV2Service } from './api-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  ApiTransferOwnership,
  DuplicateApiOptions,
  fakeApiV4,
  fakeBaseApplication,
  fakeCreateApiV4,
  fakeUpdateApiV4,
} from '../entities/management-api-v2';
import { PathToVerify } from '../entities/management-api-v2/api/verifyApiPath';

describe('ApiV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiV2Service: ApiV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiV2Service = TestBed.inject<ApiV2Service>(ApiV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('create', () => {
    it('should call the API', (done) => {
      const newApi = fakeCreateApiV4();

      apiV2Service.create(newApi).subscribe((api) => {
        expect(api.name).toEqual(newApi.name);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis`,
        method: 'POST',
      });

      req.flush(fakeCreateApiV4());
    });
  });

  describe('get', () => {
    it('should call the API', (done) => {
      const fakeApi = fakeApiV4();

      apiV2Service.get(fakeApi.id).subscribe((api) => {
        expect(api.name).toEqual(fakeApi.name);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}`,
        method: 'GET',
      });

      req.flush(fakeApiV4());
    });
  });

  describe('update', () => {
    it('should call the API', (done) => {
      const apiId = 'apiId';
      const fakeApi = fakeApiV4();
      const fakeUpdateApi = fakeUpdateApiV4();

      apiV2Service.update(apiId, fakeUpdateApi).subscribe((api) => {
        expect(api.name).toEqual(fakeApi.name);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual(fakeUpdateApi);

      req.flush(fakeApiV4());
    });
  });

  describe('delete', () => {
    it('should call the API', (done) => {
      const apiId = 'apiId';
      apiV2Service.delete(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`,
        method: 'DELETE',
      });
      req.flush(null);
    });
    it('should add a parameter to close plans', (done) => {
      const apiId = 'apiId';
      apiV2Service.delete(apiId, true).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}?closePlan=true`,
        method: 'DELETE',
      });
      req.flush(null);
    });
  });

  describe('start', () => {
    it('should call the API', (done) => {
      const fakeApi = fakeApiV4();

      apiV2Service.start(fakeApi.id).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}/_start`,
        method: 'POST',
      });

      expect(req.request.body).toEqual({});
      req.flush(fakeApi);
    });
  });

  describe('stop', () => {
    it('should call the API', (done) => {
      const fakeApi = fakeApiV4();

      apiV2Service.stop(fakeApi.id).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}/_stop`,
        method: 'POST',
      });

      expect(req.request.body).toEqual({});
      req.flush(fakeApi);
    });
  });

  describe('deploy', () => {
    it('should call the API', (done) => {
      const fakeApi = fakeApiV4();

      apiV2Service.deploy(fakeApi.id, 'Deployment label').subscribe({
        error: (err) => {
          done.fail(err);
        },
        complete: () => {
          done();
        },
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}/deployments`,
        method: 'POST',
      });

      expect(req.request.body).toEqual({
        deploymentLabel: 'Deployment label',
      });
      req.flush(fakeApi);
    });
  });

  describe('duplicate', () => {
    it('should call the API', (done) => {
      const fakeApi = fakeApiV4();

      const options: DuplicateApiOptions = { contextPath: '/duplicate', filteredFields: [] };

      apiV2Service.duplicate(fakeApi.id, options).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}/_duplicate`,
        method: 'POST',
      });

      expect(req.request.body).toEqual(options);
      req.flush(fakeApi);
    });
  });

  describe('export', () => {
    it('should call the API', (done) => {
      const fakeApi = fakeApiV4();

      apiV2Service.export(fakeApi.id).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}/_export/definition`,
        method: 'GET',
      });

      req.flush(null);
    });
  });

  describe('import', () => {
    it('should call the API', (done) => {
      const fakeApi = fakeApiV4();

      apiV2Service.import(fakeApi.id).subscribe((api) => {
        expect(api).toEqual(fakeApi);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/definition`,
        method: 'POST',
      });

      req.flush(fakeApi);
    });
  });

  describe('search', () => {
    it('should call the API', (done) => {
      const fakeApi = fakeApiV4();

      apiV2Service.search({ ids: [fakeApi.id] }).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search?page=1&perPage=10`,
        method: 'POST',
      });

      expect(req.request.body).toEqual({
        ids: [fakeApi.id],
      });
      req.flush({
        data: [fakeApi],
      });
    });

    it('should not get manage only APIs', (done) => {
      const fakeApi = fakeApiV4();

      apiV2Service.search({ ids: [fakeApi.id] }, null, 1, 9999, false).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search?page=1&perPage=9999&manageOnly=false`,
        method: 'POST',
      });

      expect(req.request.body).toEqual({
        ids: [fakeApi.id],
      });
      req.flush({
        data: [fakeApi],
      });
    });
  });

  describe('picture', () => {
    it('should update', (done) => {
      const apiId = 'apiId';

      apiV2Service.updatePicture(apiId, 'newPicture').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/picture`,
        method: 'PUT',
      });

      expect(req.request.body).toEqual('newPicture');
      req.flush(null);
    });

    it('should delete', (done) => {
      const apiId = 'apiId';

      apiV2Service.deletePicture(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/picture`,
        method: 'DELETE',
      });

      req.flush(null);
    });
  });

  describe('background', () => {
    it('should update', (done) => {
      const apiId = 'apiId';

      apiV2Service.updateBackground(apiId, 'newBackground').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/background`,
        method: 'PUT',
      });

      expect(req.request.body).toEqual('newBackground');
      req.flush(null);
    });

    it('should delete', (done) => {
      const apiId = 'apiId';

      apiV2Service.deleteBackground(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/background`,
        method: 'DELETE',
      });

      req.flush(null);
    });
  });

  describe('subscribers', () => {
    it('should call the API', (done) => {
      const apiId = 'apiId';

      apiV2Service.getSubscribers(apiId, 'my-app').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/subscribers?page=1&perPage=10&name=my-app`,
        method: 'GET',
      });

      req.flush([fakeBaseApplication()]);
    });
  });

  describe('transfer ownership', () => {
    it('should call the API', (done) => {
      const apiId = 'apiId';

      const transferOwnership: ApiTransferOwnership = {
        userId: 'user',
        userReference: 'userRef',
        userType: 'USER',
        poRole: 'role',
      };

      apiV2Service.transferOwnership(apiId, transferOwnership).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_transfer-ownership`,
        method: 'POST',
      });

      expect(req.request.body).toEqual(transferOwnership);
      req.flush(null);
    });
  });

  describe('verify API path', () => {
    it('should call the API', (done) => {
      const apiId = 'apiId';
      const paths: PathToVerify[] = [{ path: 'path', host: 'host' }];

      apiV2Service.verifyPath(apiId, paths).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/paths`,
        method: 'POST',
      });

      expect(req.request.body).toEqual({ apiId, paths });
      req.flush(null);
    });
  });

  describe('verify API hosts', () => {
    it('should call the API', (done) => {
      const apiId = 'apiId';
      const hosts = ['host1', 'host2'];

      apiV2Service.verifyHosts(apiId, hosts).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`,
        method: 'POST',
      });

      expect(req.request.body).toEqual({ apiId, hosts });
      req.flush(null);
    });
  });

  describe('getLastApiFetch', () => {
    it('should call the API', () => {
      const fakeApi = fakeApiV4({ id: 'my-api-id' });

      const done: string[] = [];
      // First call
      apiV2Service.getLastApiFetch(fakeApi.id).subscribe((api) => {
        done.push(`first getLastApiFetch: last api "${api.name}" fetch`);
      });

      // Only one call as the second one should be cached
      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}`,
          method: 'GET',
        })
        .flush(fakeApi);
      expect(done).toEqual(['first getLastApiFetch: last api "🪐 Planets" fetch']);

      apiV2Service.getLastApiFetch(fakeApi.id).subscribe((api) => {
        done.push(`second getLastApiFetch: last api "${api.name}" fetch`);
      });
      expect(done).toEqual(['first getLastApiFetch: last api "🪐 Planets" fetch', 'second getLastApiFetch: last api "🪐 Planets" fetch']);

      // On new get with unmodified api, no new event
      apiV2Service.get(fakeApi.id).subscribe((api) => {
        done.push(`get done: with unmodified api "${api.name}"`);
      });
      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}`,
          method: 'GET',
        })
        .flush(fakeApi);
      expect(done).toEqual([
        'first getLastApiFetch: last api "🪐 Planets" fetch',
        'second getLastApiFetch: last api "🪐 Planets" fetch',
        'get done: with unmodified api "🪐 Planets"',
      ]);

      // On new get with modified api, new event
      apiV2Service.get(fakeApi.id).subscribe((api) => {
        done.push(`get done: with modified api "${api.name}"`);
      });
      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}`,
          method: 'GET',
        })
        .flush({
          ...fakeApi,
          name: 'Name updated',
        });

      expect(done).toEqual([
        'first getLastApiFetch: last api "🪐 Planets" fetch',
        'second getLastApiFetch: last api "🪐 Planets" fetch',
        'get done: with unmodified api "🪐 Planets"',
        'first getLastApiFetch: last api "Name updated" fetch',
        'second getLastApiFetch: last api "Name updated" fetch',
        'get done: with modified api "Name updated"',
      ]);
    });

    it('should not use cache if apiId is different', () => {
      const fakeApiOne = fakeApiV4({ id: 'my-api-id' });
      const fakeApiTwo = fakeApiV4({ id: 'my-another-api-id' });

      const done: string[] = [];
      // First call
      apiV2Service
        .getLastApiFetch(fakeApiOne.id)
        .pipe(take(1))
        .subscribe((api) => {
          expect(api.name).toEqual(fakeApiOne.name);
          done.push('first');
        });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApiOne.id}`,
          method: 'GET',
        })
        .flush(fakeApiOne);

      // Second call
      apiV2Service
        .getLastApiFetch(fakeApiTwo.id)
        .pipe(take(1))
        .subscribe((api) => {
          expect(api.name).toEqual(fakeApiTwo.name);
          done.push('second');
        });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApiTwo.id}`,
          method: 'GET',
        })
        .flush(fakeApiTwo);

      expect(done).toEqual(['first', 'second']);
    });
  });

  describe('rollback', () => {
    it('should call the API', (done) => {
      const apiId = 'apiId';
      const eventId = 'eventId';

      apiV2Service.rollback(apiId, eventId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_rollback`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ eventId });
      req.flush(null);

      // Refresh the API
      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`,
          method: 'GET',
        })
        .flush(fakeApiV4());
    });
  });

  describe('importSwaggerApi', () => {
    it('should call the API', (done) => {
      const descriptor = 'dumb descriptor';

      apiV2Service.importSwaggerApi({ payload: descriptor, withDocumentation: false }).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_import/swagger`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ payload: descriptor, withDocumentation: false });
      req.flush(null);
    });
  });

  describe('refreshLastApiFetch', () => {
    it('should refresh the API', () => {
      const fakeApi = fakeApiV4({ id: 'my-api-id' });

      const done: string[] = [];
      // First call
      apiV2Service.getLastApiFetch(fakeApi.id).subscribe((api) => {
        done.push(`getLastApiFetch: last api "${api.name}" fetch`);
      });

      // Only one call as the second one should be cached
      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}`,
          method: 'GET',
        })
        .flush(fakeApi);

      apiV2Service.refreshLastApiFetch().subscribe({
        complete: () => {
          done.push('refreshLastApiFetch: api refreshed');
        },
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${fakeApi.id}`,
          method: 'GET',
        })
        .flush({
          ...fakeApi,
          name: 'Name updated',
        });

      expect(done).toEqual([
        'getLastApiFetch: last api "🪐 Planets" fetch',
        'getLastApiFetch: last api "Name updated" fetch',
        'refreshLastApiFetch: api refreshed',
      ]);
    });

    it('should not call the api', (done) => {
      apiV2Service.refreshLastApiFetch().subscribe({
        error: (err) => {
          done.fail(err);
        },
        complete: () => {
          done();
        },
      });

      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/apis`);
    });
  });
});
