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

import { EnvironmentIdentityProviderService } from './environment-identity-provider.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeIdentityProviderActivation, IdentityProviderActivation } from '../entities/identity-provider';

describe('EnvironmentIdentityProviderService', () => {
  let httpTestingController: HttpTestingController;
  let environmentIdentityProviderService: EnvironmentIdentityProviderService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    environmentIdentityProviderService = TestBed.inject<EnvironmentIdentityProviderService>(EnvironmentIdentityProviderService);
  });

  describe('list activated', () => {
    it('should return a list of activated identity providers', done => {
      const activatedIdentityProviders: IdentityProviderActivation[] = [
        fakeIdentityProviderActivation({ identityProvider: 'gravitee-am' }),
      ];

      environmentIdentityProviderService.list().subscribe((activatedIdentityProviders: IdentityProviderActivation[]) => {
        expect(activatedIdentityProviders).toEqual(activatedIdentityProviders);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/identities`);
      expect(req.request.method).toEqual('GET');

      req.flush(activatedIdentityProviders);
    });

    it('should update a list of activated identity providers', done => {
      const activatedIdentityProviders: IdentityProviderActivation[] = [
        fakeIdentityProviderActivation({ identityProvider: 'gravitee-am' }),
      ];

      environmentIdentityProviderService
        .update(activatedIdentityProviders)
        .subscribe((activatedIdentityProviders: IdentityProviderActivation[]) => {
          expect(activatedIdentityProviders).toEqual(activatedIdentityProviders);
          done();
        });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/identities`);
      expect(req.request.method).toEqual('PUT');

      req.flush(activatedIdentityProviders);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
