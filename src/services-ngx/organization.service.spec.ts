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

import { OrganizationService } from './organization.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeIdentityProviderActivation, IdentityProviderActivation } from '../entities/identity-provider';

describe('OrganizationService', () => {
  let httpTestingController: HttpTestingController;
  let identityProviderService: OrganizationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    identityProviderService = TestBed.inject<OrganizationService>(OrganizationService);
  });

  describe('listActivatedIdentityProviders', () => {
    it('should return a list of identity providers', (done) => {
      const activatedIdentityProviders: IdentityProviderActivation[] = [
        fakeIdentityProviderActivation({ identityProvider: 'google' }),
        fakeIdentityProviderActivation({ identityProvider: 'github' }),
      ];

      identityProviderService.listActivatedIdentityProviders().subscribe((identityProviders) => {
        expect(identityProviders).toEqual(activatedIdentityProviders);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/identities`);
      expect(req.request.method).toEqual('GET');

      req.flush(activatedIdentityProviders);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
