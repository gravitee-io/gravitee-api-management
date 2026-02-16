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
import { HttpTestingController } from '@angular/common/http/testing';

import { ClientRegistrationProvidersService } from './client-registration-providers.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { ClientRegistrationProvider } from '../entities/client-registration-provider/clientRegistrationProvider';
import { fakeClientRegistrationProvider } from '../entities/client-registration-provider/clientRegistrationProvider.fixture';

describe('ClientRegistrationProviderService', () => {
  let httpTestingController: HttpTestingController;
  let clientRegistrationProvidersService: ClientRegistrationProvidersService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    clientRegistrationProvidersService = TestBed.inject<ClientRegistrationProvidersService>(ClientRegistrationProvidersService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should list providers', done => {
    const mockProviders: ClientRegistrationProvider[] = [fakeClientRegistrationProvider(), fakeClientRegistrationProvider()];

    clientRegistrationProvidersService.list().subscribe(response => {
      expect(response).toMatchObject(mockProviders);
      done();
    });

    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers`,
    });

    req.flush(mockProviders);
  });

  it('should get provider by id', done => {
    const mockProvider: ClientRegistrationProvider = fakeClientRegistrationProvider({ scopes: null });
    {
    }
    clientRegistrationProvidersService.get('foobar').subscribe(response => {
      expect(response).toMatchObject(mockProvider);
      done();
    });

    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers/foobar`,
    });

    req.flush(mockProvider);
  });

  it('should delete provider by id', done => {
    const mockProviderId = 'foobar';

    clientRegistrationProvidersService.delete(mockProviderId).subscribe(response => {
      expect(response).toMatchObject({});
      done();
    });

    const req = httpTestingController.expectOne({
      method: 'DELETE',
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers/${mockProviderId}`,
    });

    req.flush({});
  });

  it('should create provider', done => {
    const mockProvider: ClientRegistrationProvider = fakeClientRegistrationProvider();

    clientRegistrationProvidersService.create(mockProvider).subscribe(response => {
      expect(response).toMatchObject(mockProvider);
      done();
    });

    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers`,
    });

    req.flush(mockProvider);
  });

  it('should update provider', done => {
    const mockProvider: ClientRegistrationProvider = fakeClientRegistrationProvider({ scopes: null });

    clientRegistrationProvidersService.update(mockProvider).subscribe(response => {
      expect(response).toMatchObject(mockProvider);
      done();
    });

    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers/${mockProvider.id}`,
    });

    req.flush(mockProvider);
  });
});
