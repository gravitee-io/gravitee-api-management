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

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeIdentityProviderActivation, IdentityProviderActivation } from '../entities/identity-provider';
import { fakeOrganization } from '../entities/organization/organization.fixture';

describe('OrganizationService', () => {
  let httpTestingController: HttpTestingController;
  let organizationService: OrganizationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    organizationService = TestBed.inject<OrganizationService>(OrganizationService);
  });

  describe('listActivatedIdentityProviders', () => {
    it('should return a list of identity providers', done => {
      const activatedIdentityProviders: IdentityProviderActivation[] = [
        fakeIdentityProviderActivation({ identityProvider: 'google' }),
        fakeIdentityProviderActivation({ identityProvider: 'github' }),
      ];

      organizationService.listActivatedIdentityProviders().subscribe(identityProviders => {
        expect(identityProviders).toEqual(activatedIdentityProviders);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/identities`);
      expect(req.request.method).toEqual('GET');

      req.flush(activatedIdentityProviders);
    });
  });

  describe('updateActivatedIdentityProviders', () => {
    it('should send identity providers to activate', done => {
      organizationService
        .updateActivatedIdentityProviders([{ identityProvider: 'google' }, { identityProvider: 'github' }])
        .subscribe(() => {
          done();
        });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/identities`);
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual([{ identityProvider: 'google' }, { identityProvider: 'github' }]);

      req.flush(null);
    });
  });

  describe('get', () => {
    it('should call the API', done => {
      const organization = fakeOrganization();

      organizationService.get().subscribe(response => {
        expect(response).toStrictEqual(organization);
        done();
      });

      httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.org.baseURL}` }).flush(organization);
    });
  });

  describe('update', () => {
    it('should call the API', done => {
      const updatedOrganization = fakeOrganization({ description: 'Updated description' });

      organizationService.update(updatedOrganization).subscribe(() => done());

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.org.baseURL}` });
      expect(req.request.body).toStrictEqual(updatedOrganization);

      req.flush(null);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
