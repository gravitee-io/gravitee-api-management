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
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';

import { httpInterceptorProviders } from './http-interceptors';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../testing';

describe('ReplaceEnvInterceptor', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule, HttpClientTestingModule],
      providers: [httpInterceptorProviders],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should replace {:envId} in url', (done) => {
    const testUrl = 'https://test.com/{:envId}/config';

    httpClient.get<unknown>(testUrl).subscribe(() => {
      done();
    });

    httpTestingController.expectOne(`https://test.com/${CONSTANTS_TESTING.org.currentEnv.id}/config`).flush(null);
  });
});
