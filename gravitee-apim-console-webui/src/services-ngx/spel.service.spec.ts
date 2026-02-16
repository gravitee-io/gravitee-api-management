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

import { SpelService } from './spel.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeGrammar } from '../entities/spel/grammar.fixture';

describe('SpelService', () => {
  let httpTestingController: HttpTestingController;
  let spelService: SpelService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    spelService = TestBed.inject<SpelService>(SpelService);
  });

  describe('getGrammar', () => {
    it('should call the API', done => {
      const grammar = fakeGrammar();

      spelService.getGrammar().subscribe(response => {
        expect(response).toStrictEqual(grammar);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/spel/grammar`);
      expect(req.request.method).toEqual('GET');

      req.flush(grammar);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
