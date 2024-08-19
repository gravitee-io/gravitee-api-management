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
import { HttpClient, HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { ReCaptchaInterceptor } from './re-captcha.interceptor';

import { ReCaptchaService } from '../../services-ngx/re-captcha.service';

describe('ReCaptchaInterceptor', () => {
  const testUrl = 'https://test.com/config';

  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: ReCaptchaInterceptor, multi: true },
        {
          provide: ReCaptchaService,
          useValue: {
            isEnabled: () => true,
            getHeaderName: () => 'X-RECAPTCHA',
            getCurrentToken: () => 'token',
          },
        },
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

  it('should set recaptcha header', (done) => {
    httpClient.get<unknown>(testUrl).subscribe(() => {
      done();
    });

    const req = httpTestingController.expectOne(testUrl);
    expect(req.request.headers.get('X-RECAPTCHA')).toEqual('token');

    req.flush(null);
  });
});
