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

import { TopApiService } from './top-api.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { Constants } from '../entities/Constants';
import { fakeTopApi } from '../entities/top-apis/top-apis.fixture';
import { TopApi } from '../management/settings/top-apis/top-apis.model';

describe('TopApiService', () => {
  const topApiUrl = `${CONSTANTS_TESTING.env.baseURL}/configuration/top-apis/`;
  let httpTestingController: HttpTestingController;
  let service: TopApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
      ],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(TopApiService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getList', () => {
    it('should call API', () => {
      const fakeData: TopApi[] = [fakeTopApi()];

      service.getList().subscribe(res => {
        expect(res).toMatchObject(fakeData);
      });

      httpTestingController.expectOne({ method: 'GET', url: topApiUrl }).flush(fakeData);
    });
  });

  describe('create', () => {
    it('should call API', done => {
      const fakeData: TopApi = fakeTopApi({ api: 'TEST' });

      service.create(fakeData.api).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({ method: 'POST', url: topApiUrl });
      req.flush(null);
      expect(req.request.body).toEqual({
        api: 'TEST',
      });
    });
  });

  describe('update', () => {
    it('should call API', done => {
      const fakeData: TopApi[] = [fakeTopApi()];

      service.update(fakeData).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: topApiUrl,
      });
      expect(req.request.body).toEqual([{ api: 'asdf' }]);
      req.flush(null);
    });
  });

  describe('delete', () => {
    it('should call API', done => {
      const fakeData: TopApi = fakeTopApi({ api: 'test123' });

      service.delete(fakeData.api).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'DELETE',
        url: `${topApiUrl}test123`,
      });
      req.flush(null);
    });
  });
});
