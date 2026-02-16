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

import { ApiMetadataV2Service } from './api-metadata-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeMetadata, fakeMetadataResponse } from '../entities/management-api-v2/metadata/metadata.fixture';

describe('ApiMetadataV2Service', () => {
  let httpTestingController: HttpTestingController;
  let service: ApiMetadataV2Service;
  const API_ID = 'api-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiMetadataV2Service>(ApiMetadataV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('searchApiEvents', () => {
    it.each<any | jest.DoneCallback>([
      { queryParams: {}, queryURL: '?page=1&perPage=10' },
      { queryParams: { page: 2, perPage: 12 }, queryURL: '?page=2&perPage=12' },
      { queryParams: { page: 2, perPage: 12, source: 'GLOBAL' }, queryURL: '?page=2&perPage=12&source=GLOBAL' },
      { queryParams: { page: 2, perPage: 12, sortBy: '-key' }, queryURL: '?page=2&perPage=12&sortBy=-key' },
      { queryParams: { page: 2, perPage: 12, source: 'API', sortBy: 'value' }, queryURL: '?page=2&perPage=12&source=API&sortBy=value' },
      { queryParams: { source: 'GLOBAL', sortBy: '-name' }, queryURL: '?page=1&perPage=10&source=GLOBAL&sortBy=-name' },
    ])('call the service with: $queryParams should call API with: $queryURL', ({ queryParams, queryURL }: any, done: jest.DoneCallback) => {
      const fakeResponse = fakeMetadataResponse({ data: [fakeMetadata()] });
      service.search(API_ID, queryParams).subscribe(apiPlansResponse => {
        expect(apiPlansResponse.data).toEqual(fakeResponse.data);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/metadata${queryURL}`,
        method: 'GET',
      });

      req.flush(fakeResponse);
    });
  });
});
