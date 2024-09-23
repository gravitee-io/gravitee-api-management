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
import { of } from 'rxjs';
import { startWith } from 'rxjs/operators';

import { HttpErrorInterceptor } from './http-error.interceptor';

import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../testing';
import { AuthService } from '../../auth/auth.service';

describe('HttpErrorInterceptor', () => {
  const testUrl = 'https://test.com/config';

  const fakeSnackBarService = {
    error: jest.fn(),
  };
  const fakeAuthService = {
    logout: () => {
      fakeAuthServiceNbLogoutCalls++;
      return of('null').pipe(startWith(null));
    },
  };

  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let fakeAuthServiceNbLogoutCalls = 0;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true },
        {
          provide: SnackBarService,
          useValue: fakeSnackBarService,
        },
        {
          provide: AuthService,
          useValue: fakeAuthService,
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

  it('should intercept 401 error', (done) => {
    httpClient.get<unknown>(testUrl).subscribe({
      next: () => {
        fail('Should not be called');
      },
      error: () => {
        fail('Should not be called');
      },
      complete: () => {
        expect(fakeSnackBarService.error).toHaveBeenCalledWith('Custom message');
        expect(fakeAuthServiceNbLogoutCalls).toEqual(1);
        done();
      },
    });

    const req = httpTestingController.expectOne(testUrl);
    req.flush({ message: 'Custom message' }, { status: 401, statusText: 'Unauthorized' });
  });

  it('should intercept 403 error', (done) => {
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

  it('should intercept 404 error', (done) => {
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

  it('should intercept 500 error', (done) => {
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

  it('should not intercept 400 error for specific URLs', (done) => {
    httpClient.get<unknown>(`${CONSTANTS_TESTING.org.baseURL}/user`).subscribe({
      next: () => {
        fail('Should not be called');
      },
      error: (err) => {
        expect(err.status).toEqual(400);
        expect(fakeSnackBarService.error).not.toHaveBeenCalled();
        done();
      },
    });

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/user`);
    req.flush({ message: 'Custom message' }, { status: 400, statusText: 'Bad request' });
  });
});
