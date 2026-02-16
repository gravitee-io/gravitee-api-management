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

import { ApplicationMetadataService } from './application-metadata.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeMetadata, fakeNewMetadata, fakeUpdateMetadata } from '../entities/metadata/metadata.fixture';

describe('ApplicationMetadataService', () => {
  let httpTestingController: HttpTestingController;
  let applicationMetadataService: ApplicationMetadataService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    applicationMetadataService = TestBed.inject<ApplicationMetadataService>(ApplicationMetadataService);
  });

  describe('list Application metadata', () => {
    it('should call the list endpoint', done => {
      const applicationId = 'test-id';
      const metadata = [fakeMetadata({ key: 'key1' }), fakeMetadata({ key: 'key2' })];

      applicationMetadataService.listMetadata(applicationId).subscribe(metadataList => {
        expect(metadataList.length).toEqual(2);
        expect(metadataList[0].key).toEqual('key1');
        expect(metadataList[1].key).toEqual('key2');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}/metadata/`,
        method: 'GET',
      });

      req.flush(metadata);
    });
  });

  describe('create Application metadata', () => {
    it('should call the create endpoint', done => {
      const applicationId = 'test-id';
      const metadata = fakeMetadata({ key: 'created-key' });
      const newMetadata = fakeNewMetadata();

      applicationMetadataService.createMetadata(applicationId, newMetadata).subscribe(m => {
        expect(m.key).toEqual('created-key');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}/metadata/`,
        method: 'POST',
      });

      req.flush(metadata);
    });
  });

  describe('update Application metadata', () => {
    it('should call the update endpoint', done => {
      const applicationId = 'test-id';
      const metadata = fakeMetadata({ key: 'update-key', value: 'new value' });
      const updateMetadata = fakeUpdateMetadata({ key: 'update-key', value: 'new value' });

      applicationMetadataService.updateMetadata(applicationId, updateMetadata).subscribe(m => {
        expect(m.key).toEqual('update-key');
        expect(m.value).toEqual('new value');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}/metadata/update-key`,
        method: 'PUT',
      });

      req.flush(metadata);
    });
  });

  describe('delete Application metadata', () => {
    it('should call the delete endpoint', done => {
      const applicationId = 'test-id';

      applicationMetadataService.deleteMetadata(applicationId, 'metadata-key').subscribe(() => {
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}/metadata/metadata-key`,
          method: 'DELETE',
        })
        .flush({});
    });
  });
});
