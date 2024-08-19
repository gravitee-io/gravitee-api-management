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

import { HttpErrorInterceptor } from './http-error.interceptor';

import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioTestingModule } from '../testing';

describe('HttpErrorInterceptor', () => {
  const testUrl = 'https://test.com/config';

  const fakeSnackBarService = {
    error: jest.fn(),
  };

  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true },
        {
          provide: SnackBarService,
          useValue: fakeSnackBarService,
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
    jest.resetAllMocks();
  });

  it('should intercept 400 error', (done) => {
    httpClient.get<unknown>(testUrl).subscribe({
      next: () => {
        fail('Should not be called');
      },
      error: (err) => {
        expect(err.status).toEqual(400);
        expect(fakeSnackBarService.error).toHaveBeenCalledWith('Custom message');
        done();
      },
    });

    const req = httpTestingController.expectOne(testUrl);
    req.flush({ message: 'Custom message' }, { status: 400, statusText: 'Bad request' });
  });

  it('should intercept 400 error', (done) => {
    httpClient.get<unknown>(testUrl).subscribe({
      next: () => {
        fail('Should not be called');
      },
      error: (err) => {
        expect(err.status).toEqual(401);
        expect(fakeSnackBarService.error).toHaveBeenCalledWith('Custom message');
        done();
      },
    });

    const req = httpTestingController.expectOne(testUrl);
    req.flush({ message: 'Custom message' }, { status: 401, statusText: 'Unauthorized' });
  });

  it('should intercept 400 error', (done) => {
    httpClient.get<unknown>(testUrl).subscribe({
      next: () => {
        fail('Should not be called');
      },
      error: (err) => {
        expect(err.status).toEqual(403);
        expect(fakeSnackBarService.error).toHaveBeenCalledWith('Custom message');
        done();
      },
    });

    const req = httpTestingController.expectOne(testUrl);
    req.flush({ message: 'Custom message' }, { status: 403, statusText: 'Forbidden' });
  });

  it('should intercept 400 error', (done) => {
    httpClient.get<unknown>(testUrl).subscribe({
      next: () => {
        fail('Should not be called');
      },
      error: (err) => {
        expect(err.status).toEqual(404);
        expect(fakeSnackBarService.error).toHaveBeenCalledWith('Custom message');
        done();
      },
    });

    const req = httpTestingController.expectOne(testUrl);
    req.flush({ message: 'Custom message' }, { status: 404, statusText: 'Not found' });
  });

  it('should intercept 400 error', (done) => {
    httpClient.get<unknown>(testUrl).subscribe({
      next: () => {
        fail('Should not be called');
      },
      error: (err) => {
        expect(err.status).toEqual(500);
        expect(fakeSnackBarService.error).toHaveBeenCalledWith('Custom message');
        done();
      },
    });

    const req = httpTestingController.expectOne(testUrl);
    req.flush({ message: 'Custom message' }, { status: 500, statusText: 'Internal server error' });
  });
});
