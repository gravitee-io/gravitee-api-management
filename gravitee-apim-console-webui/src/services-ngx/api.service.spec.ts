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
import { omit } from 'lodash';
import { FormControl } from '@angular/forms';
import { from } from 'rxjs';

import { ApiService } from './api.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeApi } from '../entities/api/Api.fixture';
import { fakeFlowSchema } from '../entities/flow/flowSchema.fixture';
import { fakeUpdateApi } from '../entities/api/UpdateApi.fixture';
import { fakeGroupMember } from '../entities/group/groupMember.fixture';
import { fakeMetadata, fakeNewMetadata, fakeUpdateMetadata } from '../entities/metadata/metadata.fixture';
import { fakeEvent } from '../entities/event/event.fixture';

describe('ApiService', () => {
  let httpTestingController: HttpTestingController;
  let apiService: ApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiService = TestBed.inject<ApiService>(ApiService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get', () => {
    it('should call the API', done => {
      const apiId = 'fox';
      const mockApi = fakeApi();

      apiService.get(apiId).subscribe(response => {
        expect(response).toMatchObject(mockApi);
        done();
      });

      const req = httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}` });

      req.flush(mockApi);
    });
  });

  describe('getFlowSchemaForm', () => {
    it('should call the API', done => {
      const mockApiFlow = fakeFlowSchema();

      apiService.getFlowSchemaForm().subscribe(response => {
        expect(response).toEqual(mockApiFlow);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/schema`);
      expect(req.request.method).toEqual('GET');

      req.flush(mockApiFlow);
    });
  });

  describe('update', () => {
    it('should call the API', done => {
      const apiId = 'apiId';
      const apiToUpdate = fakeUpdateApi();

      apiService.update({ id: apiId, ...apiToUpdate }).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}` });
      expect(req.request.body).toEqual(omit(apiToUpdate, 'definition_context'));
      req.flush({ id: apiId, ...apiToUpdate });
    });
  });

  describe('getAll', () => {
    it('should call the API', done => {
      const mockApis = [fakeApi()];

      apiService.getAll().subscribe(response => {
        expect(response).toMatchObject(mockApis);
        done();
      });

      const req = httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/apis` });

      req.flush(mockApis);
    });

    it('should call the API with environmentId', done => {
      const mockApis = [fakeApi()];
      const environmentId = 'environmentId';

      apiService.getAll({ environmentId }).subscribe(response => {
        expect(response).toMatchObject(mockApis);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/environments/${environmentId}/apis`,
      });

      req.flush(mockApis);
    });
  });

  describe('list', () => {
    it('should call the API with pagination default params', done => {
      const mockApis = [fakeApi()];

      apiService.list().subscribe(response => {
        expect(response).toMatchObject(mockApis);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/_search/_paged?page=1&size=10&q=*`,
      });

      req.flush(mockApis);
    });

    it('should call the API with pagination query and order', done => {
      const mockApis = [fakeApi()];

      apiService.list('toto', 'name').subscribe(response => {
        expect(response).toMatchObject(mockApis);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/_search/_paged?page=1&size=10&q=toto&order=name`,
      });

      req.flush(mockApis);
    });
  });

  describe('isAPISynchronized', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService.isAPISynchronized(apiId).subscribe(response => {
        expect(response).toEqual({
          api_id: apiId,
          is_synchronized: true,
        });
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/state`);
      expect(req.request.method).toEqual('GET');

      req.flush({
        api_id: apiId,
        is_synchronized: true,
      });
    });
  });

  describe('getQualityMetrics', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService.getQualityMetrics(apiId).subscribe(response => {
        expect(response).toEqual({
          score: 80,
        });
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/quality`);
      expect(req.request.method).toEqual('GET');

      req.flush({
        score: 80,
      });
    });
  });

  describe('askForReview', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService.askForReview(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/reviews?action=ASK`);
      expect(req.request.method).toEqual('POST');

      req.flush({});
    });
  });

  describe('acceptReview', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService.acceptReview(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/reviews?action=ACCEPT`);
      expect(req.request.method).toEqual('POST');

      req.flush({});
    });
  });

  describe('rejectReview', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService.rejectReview(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/reviews?action=REJECT`);
      expect(req.request.method).toEqual('POST');

      req.flush({});
    });
  });

  describe('start', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService.start(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}?action=START`);
      expect(req.request.method).toEqual('POST');

      req.flush({});
    });
  });

  describe('stop', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService.stop(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}?action=STOP`);
      expect(req.request.method).toEqual('POST');

      req.flush({});
    });
  });

  describe('delete', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService.delete(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}`);
      expect(req.request.method).toEqual('DELETE');

      req.flush({});
    });
  });

  describe('importApiDefinition', () => {
    it('should call the API with graviteeJson', done => {
      apiService.importApiDefinition('graviteeJson', '{}').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/import?definitionVersion=2.0.0`);
      expect(req.request.method).toEqual('POST');

      req.flush({});
    });

    it('should call the API with graviteeUrl', done => {
      apiService.importApiDefinition('graviteeUrl', '{}').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/import-url?definitionVersion=2.0.0`);
      expect(req.request.method).toEqual('POST');

      req.flush({});
    });

    it('should call the API with apiId', done => {
      apiService.importApiDefinition('graviteeUrl', '{}', 'apiId').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/apiId/import-url?definitionVersion=2.0.0`);
      expect(req.request.method).toEqual('PUT');

      req.flush({});
    });
  });

  describe('importSwaggerApi', () => {
    it('should call the API with graviteeJson', done => {
      const payload = {
        payload: '{}',
        format: 'API' as const,
        type: 'INLINE' as const,
        with_documentation: true,
        with_path_mapping: true,
        with_policies: ['policy1', 'policy2'],
        with_policy_paths: true,
      };

      apiService.importSwaggerApi(payload).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/import/swagger?definitionVersion=2.0.0`);
      expect(req.request.method).toEqual('POST');

      req.flush({});
    });

    it('should call the API with apiId', done => {
      const payload = {
        payload: '{}',
        format: 'API' as const,
        type: 'INLINE' as const,
        with_documentation: true,
        with_path_mapping: true,
        with_policies: ['policy1', 'policy2'],
        with_policy_paths: true,
      };

      apiService.importSwaggerApi(payload, 'apiId').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/apiId/import/swagger?definitionVersion=2.0.0`);
      expect(req.request.method).toEqual('PUT');

      req.flush({});
    });
  });

  describe('export', () => {
    it('should call the API', done => {
      const apiId = 'apiId';

      apiService.export(apiId, ['plans'], '2.0.0').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/export?exclude=plans&version=2.0.0`,
        method: 'GET',
      });

      req.flush(new Blob(['a'], { type: 'text/json' }));
    });
  });

  describe('exportCrd', () => {
    it('should call the API', done => {
      const apiId = 'apiId';

      apiService.exportCrd(apiId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/crd`, method: 'GET' });

      req.flush(new Blob(['a'], { type: 'text/yml' }));
    });
  });

  describe('duplicate', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService
        .duplicate(apiId, {
          context_path: 'My API',
          version: '1.0.0',
          filtered_fields: ['groups'],
        })
        .subscribe(() => {
          done();
        });

      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/duplicate`, method: 'POST' });

      req.flush({});
    });
  });

  describe('contextPathValidator', () => {
    function expectVerifyContextPathPostRequest() {
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/verify`, method: 'POST' }).flush({});
    }

    function expectVerifyContextPathFailedRequest() {
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/verify`, method: 'POST' });
      req.flush({ message: 'The path [/echo/] is already covered by an other API.' }, { status: 422, statusText: 'Unprocessable Entity' });
      return;
    }

    it.each([
      ['a', null, { contextPath: 'Context path has to be more than 3 characters long.' }],
      [
        'aa',
        null,
        {
          contextPath: 'Context path has to be more than 3 characters long.',
        },
      ],
      ['aaa', expectVerifyContextPathPostRequest, null],
      ['/aaa', expectVerifyContextPathPostRequest, null],
      ['/echo', expectVerifyContextPathFailedRequest, { contextPath: 'The path [/echo/] is already covered by an other API.' }],
    ] as any[])('should validate %p contextPath', (contextPath, expectVerifyContextPathGetRequest, error, done) => {
      from(apiService.contextPathValidator({})(new FormControl(contextPath))).subscribe(result => {
        expect(result).toEqual(error);
        done();
      });

      if (expectVerifyContextPathGetRequest) {
        expectVerifyContextPathGetRequest();
      }
    });
  });

  describe('importPathMappings', () => {
    it('should call the API', done => {
      const apiId = 'api#1';
      const pageId = 'pageId';
      const apiVersion = 'apiVersion';

      apiService.importPathMappings(apiId, pageId, apiVersion).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/import-path-mappings?page=${pageId}&definitionVersion=${apiVersion}`,
        method: 'POST',
      });
      req.flush({});
    });

    it('should call the API without api version', done => {
      const apiId = 'api#1';
      const pageId = 'pageId';

      apiService.importPathMappings(apiId, pageId, null).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/import-path-mappings?page=${pageId}`,
        method: 'POST',
      });
      req.flush({});
    });
  });

  describe('transferOwnership', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiService
        .transferOwnership(apiId, {
          id: 'user#1',
          role: 'OWNER',
        })
        .subscribe(() => {
          done();
        });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/members/transfer_ownership`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({
        id: 'user#1',
        role: 'OWNER',
      });
      req.flush({});
    });
  });

  describe('getGroupIdsWithMembers', () => {
    it('should call the API', done => {
      const apiId = 'api#1';
      const fakeGroupsMember = {
        groupId1: [fakeGroupMember({ id: 'user#1' })],
        groupId2: [fakeGroupMember({ id: 'user#2' })],
      };

      apiService.getGroupIdsWithMembers(apiId).subscribe(value => {
        expect(value).toEqual(fakeGroupsMember);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/groups`,
        method: 'GET',
      });
      req.flush(fakeGroupsMember);
    });
  });

  /**
   * Metadata
   */
  describe('list API metadata', () => {
    it('should call the list endpoint', done => {
      const apiId = 'my-api';
      const metadata = [fakeMetadata({ key: 'key1' }), fakeMetadata({ key: 'key2' })];

      apiService.listMetadata(apiId).subscribe(metadataList => {
        expect(metadataList.length).toEqual(2);
        expect(metadataList[0].key).toEqual('key1');
        expect(metadataList[1].key).toEqual('key2');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/metadata/`,
        method: 'GET',
      });

      req.flush(metadata);
    });
  });

  describe('create API metadata', () => {
    it('should call the create endpoint', done => {
      const apiId = 'my-api';
      const metadata = fakeMetadata({ key: 'created-key' });
      const newMetadata = fakeNewMetadata();

      apiService.createMetadata(apiId, newMetadata).subscribe(m => {
        expect(m.key).toEqual('created-key');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/metadata/`,
        method: 'POST',
      });

      req.flush(metadata);
    });
  });

  describe('update API metadata', () => {
    it('should call the update endpoint', done => {
      const apiId = 'my-api';
      const metadata = fakeMetadata({ key: 'update-key', value: 'new value' });
      const updateMetadata = fakeUpdateMetadata({ key: 'update-key', value: 'new value' });

      apiService.updateMetadata(apiId, updateMetadata).subscribe(m => {
        expect(m.key).toEqual('update-key');
        expect(m.value).toEqual('new value');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/metadata/update-key`,
        method: 'PUT',
      });

      req.flush(metadata);
    });
  });

  describe('delete API metadata', () => {
    it('should call the delete endpoint', done => {
      const apiId = 'my-api';

      apiService.deleteMetadata(apiId, 'metadata-key').subscribe(() => {
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/metadata/metadata-key`,
          method: 'DELETE',
        })
        .flush({});
    });
  });

  /**
   * Events **
   */
  describe('list API events', () => {
    it('should call the list events endpoint', done => {
      const apiId = 'my-api';
      const events = [
        fakeEvent({ type: 'PUBLISH_API', created_at: new Date('2020-02-02T20:20:02') }),
        fakeEvent({ type: 'START_API', created_at: new Date('2020-02-02T20:22:02') }),
        fakeEvent({ type: 'STOP_API', created_at: new Date('2020-02-22T20:22:02') }),
      ];

      apiService.getApiEvents(apiId, ['PUBLISH_API', 'START_API', 'STOP_API']).subscribe(events => {
        expect(events.length).toEqual(3);
        expect(events[0].type).toEqual('PUBLISH_API');
        expect(events[1].type).toEqual('START_API');
        expect(events[2].type).toEqual('STOP_API');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/events?type=PUBLISH_API,START_API,STOP_API`,
        method: 'GET',
      });

      req.flush(events);
    });
  });
});
