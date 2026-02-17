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
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { Observable } from 'rxjs';

import { OrganizationEnvironmentGuard } from './organization-environment.guard';

import { CONSTANTS_TESTING } from '../../shared/testing';
import { Constants } from '../../entities/Constants';
import { Environment } from '../../entities/environment/environment';

describe('OrganizationEnvironmentGuard', () => {
  let httpTestingController: HttpTestingController;
  let constants: Constants;

  beforeEach(() => {
    constants = {
      ...CONSTANTS_TESTING,
      org: {
        ...CONSTANTS_TESTING.org,
        environments: null,
        currentEnv: null,
      },
    };

    TestBed.configureTestingModule({
      providers: [{ provide: Constants, useValue: constants }, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should fetch environments and set currentEnv when not already loaded', (done) => {
    const fakeEnvironments: Environment[] = [
      { id: 'env-1', name: 'Environment 1', organizationId: 'org-1', hrids: ['env-1'] },
      { id: 'env-2', name: 'Environment 2', organizationId: 'org-1', hrids: ['env-2'] },
    ];

    const result = TestBed.runInInjectionContext(() => OrganizationEnvironmentGuard(null, null));

    (result as Observable<boolean>).subscribe((canActivate) => {
      expect(canActivate).toBe(true);
      expect(constants.org.environments).toEqual(fakeEnvironments);
      expect(constants.org.currentEnv).toEqual(fakeEnvironments[0]);
      done();
    });

    const req = httpTestingController.expectOne(`${constants.org.baseURL}/environments`);
    expect(req.request.method).toEqual('GET');
    req.flush(fakeEnvironments);
  });

  it('should skip fetch when environments are already loaded', () => {
    const existingEnv: Environment = { id: 'existing-env', name: 'Existing', organizationId: 'org-1' };
    constants.org.environments = [existingEnv];
    constants.org.currentEnv = existingEnv;

    const result = TestBed.runInInjectionContext(() => OrganizationEnvironmentGuard(null, null));

    (result as Observable<boolean>).subscribe((canActivate) => {
      expect(canActivate).toBe(true);
    });

    httpTestingController.expectNone(`${constants.org.baseURL}/environments`);
    expect(constants.org.currentEnv).toEqual(existingEnv);
  });

  it('should still activate route when environment list is empty', (done) => {
    const result = TestBed.runInInjectionContext(() => OrganizationEnvironmentGuard(null, null));

    (result as Observable<boolean>).subscribe((canActivate) => {
      expect(canActivate).toBe(true);
      expect(constants.org.environments).toBeNull();
      expect(constants.org.currentEnv).toBeNull();
      done();
    });

    const req = httpTestingController.expectOne(`${constants.org.baseURL}/environments`);
    req.flush([]);
  });

  it('should still navigate when environment fetch fails', (done) => {
    const result = TestBed.runInInjectionContext(() => OrganizationEnvironmentGuard(null, null));

    (result as Observable<boolean>).subscribe((canActivate) => {
      expect(canActivate).toBe(true);
      expect(constants.org.environments).toBeNull();
      expect(constants.org.currentEnv).toBeNull();
      done();
    });

    httpTestingController.expectOne(`${constants.org.baseURL}/environments`).flush({}, { status: 500, statusText: 'Server Error' });
  });
});
