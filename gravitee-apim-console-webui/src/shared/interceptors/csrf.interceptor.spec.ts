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
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpClient, HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, withXsrfConfiguration } from '@angular/common/http';
import { catchError, switchMap } from 'rxjs/operators';

import { CsrfInterceptor } from './csrf.interceptor';

describe('CsrfInterceptor', () => {
  const testUrl = 'https://test.com/config';

  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: CsrfInterceptor, multi: true },
        provideHttpClient(
          withInterceptorsFromDi(),
          // Explicitly disable automatic csrf handling as it will not work for cross-domain (using custom csrf interceptor).
          withXsrfConfiguration({
            cookieName: 'none',
            headerName: 'none',
          }),
        ),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('store a token on first response and use it for other requests', (done) => {
    httpClient
      .get<unknown>(testUrl)
      .pipe(switchMap(() => httpClient.get<unknown>(testUrl)))
      .subscribe(() => {
        done();
      });

    // Return a Xsrf Token on first request
    httpTestingController.expectOne(testUrl).flush(
      {},
      {
        headers: {
          'X-Xsrf-Token': 'a token',
        },
      },
    );

    const req2 = httpTestingController.expectOne(testUrl);
    expect(req2.request.headers.get('X-Xsrf-Token')).toEqual('a token');
    req2.flush({});
  });

  it('store a token on error response and use it for other requests', (done) => {
    httpClient
      .get<unknown>(testUrl)
      .pipe(catchError(() => httpClient.get<unknown>(testUrl)))
      .subscribe(() => {
        done();
      });

    // Return a Xsrf Token on error request
    httpTestingController.expectOne(testUrl).error(new ErrorEvent('Bad Request!'), {
      headers: {
        'X-Xsrf-Token': 'a token',
      },
    });

    const req2 = httpTestingController.expectOne(testUrl);
    expect(req2.request.headers.get('X-Xsrf-Token')).toEqual('a token');
    req2.flush({});
  });
});
