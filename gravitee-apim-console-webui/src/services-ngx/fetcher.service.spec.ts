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

import { FetcherService } from './fetcher.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeFetcherListItem } from '../entities/fetcher/fetcher.fixture';

describe('FetcherService', () => {
  let httpTestingController: HttpTestingController;
  let fetcherService: FetcherService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    fetcherService = TestBed.inject<FetcherService>(FetcherService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list FetcherListItem', () => {
    it('should call the create endpoint', (done) => {
      const id1 = 'id1';
      const id2 = 'id2';
      const schema1 =
        '{"type": "object","title": "http","properties": {"url": {"title": "URL","description": "description1","type": "string"}}}';
      const schema2 =
        '{"type": "object","title": "http","properties": {"url": {"title": "URL","description": "description2","type": "string"}}}';

      const fetcherListItem = [fakeFetcherListItem({ id: id1, schema: schema1 }), fakeFetcherListItem({ id: id2, schema: schema2 })];

      fetcherService.getList().subscribe((fetcherListItem) => {
        expect(fetcherListItem.length).toEqual(2);
        expect(fetcherListItem[0].schema).toEqual(schema1);
        expect(fetcherListItem[1].schema).toEqual(schema2);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/fetchers?expand=schema`,
        method: 'GET',
      });

      req.flush(fetcherListItem);
    });
  });
});
