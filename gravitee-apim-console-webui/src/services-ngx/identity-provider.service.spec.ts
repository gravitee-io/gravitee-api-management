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

import { IdentityProviderService } from './identity-provider.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeIdentityProviderListItem } from '../entities/identity-provider/identityProviderListItem.fixture';
import { fakeIdentityProvider, IdentityProviderListItem } from '../entities/identity-provider';
import { fakeNewIdentityProvider } from '../entities/identity-provider/newIdentityProvider.fixture';

describe('IdentityProviderService', () => {
  let httpTestingController: HttpTestingController;
  let identityProviderService: IdentityProviderService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    identityProviderService = TestBed.inject<IdentityProviderService>(IdentityProviderService);
  });

  describe('list', () => {
    it('should return a list of identity providers', done => {
      const identityProviders: IdentityProviderListItem[] = [
        fakeIdentityProviderListItem({ id: 'google', type: 'GOOGLE' }),
        fakeIdentityProviderListItem({ id: 'github', type: 'GITHUB' }),
      ];

      identityProviderService.list().subscribe(identityProviders => {
        expect(identityProviders).toEqual(identityProviders);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities`);
      expect(req.request.method).toEqual('GET');

      req.flush(identityProviders);
    });
  });

  describe('delete', () => {
    it('should send a DELETE request', done => {
      const identityProviderToDelete: IdentityProviderListItem = fakeIdentityProviderListItem({ id: 'github', type: 'GITHUB' });

      identityProviderService.delete(identityProviderToDelete.id).subscribe(identityProviders => {
        expect(identityProviders).toEqual(identityProviders);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.baseURL}/configuration/identities/${identityProviderToDelete.id}`,
      );
      expect(req.request.method).toEqual('DELETE');

      req.flush(null);
    });
  });

  describe('create', () => {
    it('should send a POST request', done => {
      const identityProviderToCreate = fakeNewIdentityProvider({ type: 'GITHUB' });

      const identityProviderCreated = fakeIdentityProvider({ id: 'newId', ...identityProviderToCreate });

      identityProviderService.create(identityProviderToCreate).subscribe(identityProvider => {
        expect(identityProvider).toEqual(identityProviderCreated);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(identityProviderToCreate);

      req.flush(identityProviderCreated);
    });
  });

  describe('update', () => {
    it('should send a PUT request', done => {
      const identityProviderToUpdate = fakeIdentityProvider({ type: 'GITHUB' });

      const identityProviderUpdated = fakeIdentityProvider({ id: 'newId', ...identityProviderToUpdate });

      identityProviderService.update(identityProviderToUpdate).subscribe(identityProvider => {
        expect(identityProvider).toEqual(identityProviderUpdated);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.baseURL}/configuration/identities/${identityProviderToUpdate.id}`,
      );
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toStrictEqual({
        configuration: identityProviderToUpdate.configuration,
        description: identityProviderToUpdate.description,
        emailRequired: identityProviderToUpdate.emailRequired,
        enabled: identityProviderToUpdate.enabled,
        groupMappings: identityProviderToUpdate.groupMappings,
        name: identityProviderToUpdate.name,
        roleMappings: identityProviderToUpdate.roleMappings,
        syncMappings: identityProviderToUpdate.syncMappings,
        userProfileMapping: identityProviderToUpdate.userProfileMapping,
      });

      req.flush(identityProviderUpdated);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
